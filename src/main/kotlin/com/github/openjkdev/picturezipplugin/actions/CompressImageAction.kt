package com.github.openjkdev.picturezipplugin.actions;

import com.github.openjkdev.picturezipplugin.http.HttpUtils
import com.github.openjkdev.picturezipplugin.services.BaiduTranslateService
import com.github.openjkdev.picturezipplugin.thread.ThreadPools
import com.github.openjkdev.picturezipplugin.utils.*
import com.google.gson.JsonParser
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import java.awt.*
import java.io.File
import java.net.URL
import java.util.*
import javax.swing.*
import kotlin.math.max

class CompressImageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        CustomDialog(e).apply {
            title = "压缩图片"
        }.showAndGet()
    }

    /**
     * 图片压缩弹窗
     */
    class CustomDialog(private val actionEvent: AnActionEvent) : DialogWrapper(true) {
        //原始图片
        private var originImageLabel: JLabel = createImageLabel()
        private var originImageInfoLabel: JLabel = createImageLabel()
        private var originImageNameLabel: JLabel = createImageLabel()

        //压缩后的图片
        private var compressImageLabel: JLabel = createImageLabel()
        private var compressImageInfoLabel: JLabel = createImageLabel()
        private var compressImageTipLabel: JLabel = createImageLabel()
        private var compressImageNameLabel: JLabel = createImageLabel()

        //缓存的图片地址
        private var cacheFile: String? = null
        private var fileName: String? = null

        init {
            init()
        }

        override fun doCancelAction() {
            super.doCancelAction()
            ThreadPools.instance().asyncExecutor().shutdown()
        }

        override fun doOKAction() {
            if (cacheFile.isNullOrEmpty()) {
                Messages.showErrorDialog("请先上传图片", "温馨提示")
                return
            }
            val configBean = CacheUtils.parseConfig(CacheUtils.readConfig())
            val savePath = configBean.savePath?.find {
                it.select
            }
            val prefix = configBean.prefixList?.find {
                it.select
            }?.value ?: ""
            if (savePath == null) {
                Messages.showErrorDialog("请选择图片保存路径", "温馨提示")
                return
            }
            if (savePath.path.isNullOrEmpty()) {
                Messages.showErrorDialog("图片保存路径配置异常", "温馨提示")
                return
            }

            val destPath = savePath.path!! + File.separator + prefix + fileName
            val destFile = File(destPath)
            destFile.createNewFile()
            FileUtils.copyFile(File(cacheFile!!), destFile)
            super.doOKAction()

        }

        override fun createCenterPanel(): JComponent? {
            val project = actionEvent.project
            if (Objects.isNull(project)) {
                return null
            }
            //根布局
            val boxRoot = Box.createVerticalBox().apply {
                preferredSize = Dimension(800, 600)
            }
            val panel = JPanel().apply {
                isVisible = true
                add(boxRoot)
            }
            val boxTitle = Box.createHorizontalBox().apply {
                preferredSize = Dimension(800, 40)
            }
            val boxPath = Box.createHorizontalBox().apply {
                preferredSize = Dimension(800, 40)
            }
            val boxImage = Box.createHorizontalBox().apply {
                preferredSize = Dimension(800, 340)
            }
            boxRoot.add(boxTitle)
            boxRoot.add(boxPath)
            boxRoot.add(boxImage)
            //创建选择文件的按钮布局
            val fileChooserBtn = createSelectImageButton(project!!) { path, name ->
                if (!FileUtils.fileIsImage(path)) {
                    JOptionPane.showMessageDialog(panel, "仅支持 png、jpg、jpeg 图片的压缩")
                    return@createSelectImageButton
                }
                cacheFile = null
                fileName = null
                val originFile = File(path)
                val point = getImageScaleSize(path, 300f)
                val icon = createImageIcon(path, true)
                icon.image = icon.image.getScaledInstance(point.x, point.y, Image.SCALE_SMOOTH)
                originImageLabel.icon = icon
                originImageInfoLabel.text = "大小：" + FileUtils.getFormatSize(FileUtils.getFolderSize(originFile))
                originImageNameLabel.text = "原文件名：" + originFile.name
                fileName = originFile.name
                compressImageLabel.icon = MyIcons.loadIcon
                compressImageInfoLabel.text = ""
                compressImageTipLabel.text = ""
                compressImageNameLabel.text = ""
                requestTranslateFileName(originFile.nameWithoutExtension, originFile.extension) { transName, status ->
                    val configBean = CacheUtils.parseConfig(CacheUtils.readConfig())
                    val prefix = configBean.prefixList?.find {
                        it.select
                    }?.value ?: ""
                    when (status) {
                        0 -> {
                            compressImageNameLabel.text = "保存文件名：$prefix$transName(未配置翻译)"
                        }

                        1 -> {
                            compressImageNameLabel.text = "保存文件名：$prefix$transName(翻译失败)"
                        }

                        else -> {
                            fileName = transName
                            compressImageNameLabel.text = "保存文件名：$prefix$transName(翻译后)"
                        }
                    }
                }
                requestCompressImage(path) { url, compress, msg ->
                    msg?.let {
                        JOptionPane.showMessageDialog(panel, it)
                    }
                    compress?.let {
                        compressImageInfoLabel.text = it
                    }
                    url?.let {
                        val compressIcon = createImageIcon(url, false)
                        compressIcon.image = compressIcon.image.getScaledInstance(point.x, point.y, Image.SCALE_SMOOTH)
                        compressImageLabel.icon = compressIcon
                        requestDownImage(it, CacheUtils.TEMP_IMAGE_PATH) { result, msg ->
                            if (result) {
                                cacheFile = CacheUtils.TEMP_IMAGE_PATH
                                compressImageTipLabel.text = "已缓存"
                            } else {
                                compressImageTipLabel.text = "缓存失败"
                            }
                            //JOptionPane.showMessageDialog(panel, msg+"-"+project?.projectFilePath)
                        }
                    }
                }
            }.apply {
                setSize(100, 40)
            }
            //添加选择图片按钮布局
            boxTitle.add(fileChooserBtn)
            boxTitle.add(JButton("配置").apply {
                addActionListener {
                    val data = Messages.showMultilineInputDialog(project, "修改对应字段来更改相关配置" +
                            "\n属性介绍：\nenableTranslate:是否开启翻译，开启翻译后将自动将文件名翻译成英文" +
                            "\ntranslateKey:百度翻译的 Key，需要在百度翻译中申请" +
                            "\nsavePath:图片压缩后将要保存的位置" +
                            "\nname:路径的名称，要绝对唯一" +
                            "\npath:保存的绝对路径" +
                            "\nprefixList:文件默认添加的前缀列表" +
                            "\npreName:前缀选项显示名称" +
                            "\nvalue:文件前缀的真实名称",
                            "配置信息", CacheUtils.readConfig(), null, object : InputVerifier(), InputValidator {
                        override fun verify(input: JComponent?): Boolean {
                            return true
                        }

                        override fun checkInput(inputString: String?): Boolean {
                            return true
                        }

                        override fun canClose(inputString: String?): Boolean {
                            if (inputString.isNullOrEmpty()) {
                                return false
                            }
                            if (!JsonParser.parseString(inputString).isJsonObject) {
                                return false
                            }
                            return true
                        }

                    }) ?: ""
                    if (!CacheUtils.isErrorConfig(data)) {
                        CacheUtils.writeConfig(data)
                        //添加图片存放地址选项
                        boxPath.removeAll()
                        boxPath.add(getImagePathView())
                        boxPath.add(getPrefixView())
                    }
                }
            })
            //添加图片存放地址选项
            boxPath.add(getImagePathView())
            boxPath.add(getPrefixView())
            //添加原图布局
            boxImage.add(JPanel().apply {
                isVisible = true
                layout = GridBagLayout()
                background = Color.GRAY
                val gbc = GridBagConstraints()
                gbc.fill = GridBagConstraints.VERTICAL
                gbc.gridx = 0
                gbc.gridy = 0
                add(JLabel("原图片").apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                })
                gbc.gridx = 0
                gbc.gridy = 1
                add(JPanel().apply {
                    preferredSize = Dimension(300, 300)
                    originImageLabel.apply {
                        preferredSize = Dimension(300, 300)
                        verticalAlignment = SwingConstants.CENTER
                        horizontalAlignment = SwingConstants.CENTER
                    }
                    add(originImageLabel)
                    background = Color.darkGray
                }, gbc)
                gbc.gridx = 0
                gbc.gridy = 2
                add(originImageInfoLabel.apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                }, gbc)
                gbc.gridx = 0
                gbc.gridy = 3
                add(originImageNameLabel.apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                }, gbc)
                gbc.gridx = 0
                gbc.gridy = 4
                add(JLabel().apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                }, gbc)
            })

            //添加压缩后图片的布局
            boxImage.add(JPanel().apply {
                isVisible = true
                layout = GridBagLayout()
                background = Color.GRAY
                val gbc = GridBagConstraints()
                gbc.fill = GridBagConstraints.VERTICAL
                gbc.gridx = 0
                gbc.gridy = 0
                add(JLabel("压缩后图片").apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                })
                gbc.gridx = 0
                gbc.gridy = 1
                add(JPanel().apply {
                    preferredSize = Dimension(300, 300)
                    compressImageLabel.apply {
                        preferredSize = Dimension(300, 300)
                        verticalAlignment = SwingConstants.CENTER
                        horizontalAlignment = SwingConstants.CENTER
                    }
                    add(compressImageLabel)
                    background = Color.darkGray
                }, gbc)
                gbc.gridx = 0
                gbc.gridy = 2
                add(compressImageInfoLabel.apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                }, gbc)

                gbc.gridx = 0
                gbc.gridy = 3
                add(compressImageTipLabel.apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                }, gbc)
                gbc.gridx = 0
                gbc.gridy = 4
                add(compressImageNameLabel.apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                }, gbc)
            })
            return panel
        }

        /**
         * 创建展示图片标签
         */
        private fun createImageLabel(): JLabel {
            val imageLabel = JLabel()
            return imageLabel
        }

        /**
         * 创建 图片
         */
        private fun createImageIcon(src: String, isFile: Boolean): ImageIcon {
            return if (isFile) {
                ImageIcon(src)
            } else {
                ImageIcon(URL(src))
            }
        }

        /**
         * 创建选择图片的按钮
         */
        private fun createSelectImageButton(project: Project, callback: (String, String) -> Unit): JButton {
            val fileChooseBtn = JButton("选择压缩图片")
            fileChooseBtn.addActionListener {
                val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
                FileChooser.chooseFile(descriptor, project, null) {
                    callback.invoke(it.path, it.name)
                }
            }
            return fileChooseBtn
        }

        /**
         * 请求翻译文件名称
         */
        private fun requestTranslateFileName(name: String, extension: String, callback: (String, Int) -> Unit) {
            val configBean = CacheUtils.parseConfig(CacheUtils.readConfig())
            if (configBean.enableTranslate == false || configBean.translateKey.isNullOrEmpty() || configBean.translateAppId.isNullOrEmpty()) {
                callback.invoke("$name.$extension", 0)//0-没有配置翻译
                return
            }
            ThreadPools.instance().asyncExecutor().execute {
                val baiduTranslateService = BaiduTranslateService(configBean.translateAppId, configBean.translateKey)
                val result = baiduTranslateService.parseResult(baiduTranslateService.translate(name, "zh", "en"))
                if (result.isNullOrEmpty()) {
                    callback.invoke("$name.$extension", 1)//翻译失败
                } else {
                    callback.invoke("$result.$extension", 2)//翻译成功
                }
            }
        }

        /**
         * 请求压缩图片
         */
        private fun requestCompressImage(path: String, callback: (String?, String?, String?) -> Unit) {
            ThreadPools.instance().asyncExecutor().execute {
                HttpUtils.uploadFile(HttpUtils.uploadUrl, path) {
                    val data = JsonParser.parseString(it).asJsonObject
                    if (data.get("code").asInt == 0) {
                        val content = data.get("data").asJsonObject
                        val url = content.get("output").asJsonObject.get("url").asString
                        val size = content.get("output").asJsonObject.get("size").asLong
                        val ratio = content.get("output").asJsonObject.get("ratio").asDouble
                        val compress = "大小：" + FileUtils.getFormatSize(size) + "  压缩率：" + FormatUtils.formatAmount2("${ratio * 100}") + "%"
                        callback.invoke(url, compress, null)
                    } else {
                        val content = data.get("data").asString
                        callback.invoke(null, null, content)
                    }
                }
            }
        }

        /**
         * 请求下载压缩后的图片
         */
        private fun requestDownImage(url: String, saveFile: String, callback: (Boolean, String) -> Unit) {
            val file = File(saveFile)
            if (!file.exists()) {
                file.createNewFile()
            }
            ThreadPools.instance().asyncExecutor().execute {
                HttpUtils.downFile(url, saveFile) {
                    val data = JsonParser.parseString(it).asJsonObject
                    if (data.get("code").asInt == 0) {
                        callback.invoke(true, file.absolutePath)
                    } else {
                        val content = data.get("data").asString
                        callback.invoke(false, content)
                    }
                }
            }
        }

        /**
         * 获取图片缩放尺寸
         */
        private fun getImageScaleSize(srcPath: String, boxSize: Float): Point {
            var w = 0
            var h = 0
            ImageUtils(File(srcPath)).apply {
                w = width
                h = height
            }
            if (w <= 300 && h <= 300) {
                return Point(w, h)
            } else {
                val max = max(w, h)
                val scale = max / boxSize
                return Point((w / scale).toInt(), (h / scale).toInt())
            }
        }

        /**
         * 选择图片地址的组件
         */
        private fun getImagePathView(): JComponent {
            return JPanel().apply {
                isVisible = true
                layout = FlowLayout()
                add(JLabel("选择图片存放地址"))
                val configBean = CacheUtils.parseConfig(CacheUtils.readConfig())
                val savePathList = configBean.savePath
                val buttonGroup = ButtonGroup()
                savePathList?.forEach {
                    val jRadioButton = JRadioButton(it.name, it.select)
                    add(jRadioButton)
                    buttonGroup.add(jRadioButton)
                    jRadioButton.addActionListener {
                        if (jRadioButton.isSelected) {
                            savePathList.forEach { image ->
                                image.select = image.name == jRadioButton.text
                            }
                            CacheUtils.writeConfig(CacheUtils.configToJsonStr(configBean))
                        }
                    }
                }
            }
        }

        /**
         * 选择文件前缀的组件
         */
        private fun getPrefixView(): JComponent {
            return JPanel().apply {
                isVisible = true
                layout = FlowLayout()
                add(JLabel("选择文件保存的前缀名称"))
                val configBean = CacheUtils.parseConfig(CacheUtils.readConfig())
                val prefixList = configBean.prefixList
                val buttonGroup = ButtonGroup()
                prefixList?.forEach {
                    val jRadioButton = JRadioButton(it.preName, it.select)
                    add(jRadioButton)
                    buttonGroup.add(jRadioButton)
                    jRadioButton.addActionListener {
                        if (jRadioButton.isSelected) {
                            prefixList.forEach { prefix ->
                                prefix.select = prefix.preName === jRadioButton.text
                                if (prefix.select) {
                                    compressImageNameLabel.text = "保存文件名：${prefix.value}$fileName(翻译后)"
                                }
                            }
                            CacheUtils.writeConfig(CacheUtils.configToJsonStr(configBean))
                        }
                    }
                }

            }
        }
    }


}

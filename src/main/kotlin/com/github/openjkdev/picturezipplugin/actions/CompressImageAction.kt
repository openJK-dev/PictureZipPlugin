package com.github.openjkdev.picturezipplugin.actions;

import com.github.openjkdev.picturezipplugin.http.HttpUtils
import com.github.openjkdev.picturezipplugin.thread.ThreadPools
import com.github.openjkdev.picturezipplugin.utils.FileUtils
import com.github.openjkdev.picturezipplugin.utils.FormatUtils
import com.github.openjkdev.picturezipplugin.utils.ImageUtils
import com.github.openjkdev.picturezipplugin.utils.MyIcons
import com.google.gson.JsonParser
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
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
            setSize(800, 540)
        }.show()
    }

    /**
     * 图片压缩弹窗
     */
    class CustomDialog(private val actionEvent: AnActionEvent) : DialogWrapper(true) {
        //原始图片
        private var originImageLabel: JLabel = createImageLabel()
        private var originImageInfoLabel: JLabel = createImageLabel()

        //压缩后的图片
        private var compressImageLabel: JLabel = createImageLabel()
        private var compressImageInfoLabel: JLabel = createImageLabel()

        init {
            init()
        }

        override fun createCenterPanel(): JComponent? {
            val project = actionEvent.project
            if (Objects.isNull(project)) {
                return null
            }
            //根布局
            val panel = JPanel().apply {
                isVisible = true
                setSize(800, 540)
            }

            //添加列名布局
            panel.add(JLabel("原图片").apply { preferredSize = Dimension(300, 40) })
            panel.add(JLabel("压缩后图片").apply { preferredSize = Dimension(300, 40) })
            panel.add(JLabel("选择图片").apply { preferredSize = Dimension(100, 40) })

            //创建选择文件的按钮布局
            val fileChooserBtn = createSelectImageButton(project!!) { path, name ->
                val point = getImageScaleSize(path, 300f)
                val icon = createImageIcon(path, true)
                icon.image = icon.image.getScaledInstance(point.x, point.y, Image.SCALE_SMOOTH)
                originImageLabel.icon = icon
                originImageInfoLabel.text = "大小：" + FileUtils.getFormatSize(FileUtils.getFolderSize(File(path)))
                compressImageLabel.icon = MyIcons.loadIcon
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
                        requestDownImage(it, "C:\\Users\\Administrator\\Downloads\\newPng.png") { _, msg ->
                            //JOptionPane.showMessageDialog(panel, msg+"-"+project?.projectFilePath)
                        }
                    }
                }
            }.apply {
                setSize(100, 40)
            }

            //添加原图布局
            panel.add(JPanel().apply {
                preferredSize = Dimension(300, 340)
                isVisible = true
                layout = GridBagLayout()
                val gbc = GridBagConstraints()
                gbc.fill = GridBagConstraints.VERTICAL
                gbc.gridx = 0
                gbc.gridy = 0

                add(JPanel().apply {
                    preferredSize = Dimension(300, 300)
                    originImageLabel.apply {
                        preferredSize = Dimension(300, 300)
                        verticalAlignment = SwingConstants.CENTER
                        horizontalAlignment = SwingConstants.CENTER
                    }
                    add(originImageLabel)
                }, gbc)
                gbc.gridx = 0
                gbc.gridy = 1
                add(originImageInfoLabel.apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                }, gbc)
            })

            //添加压缩后图片的布局
            panel.add(JPanel().apply {
                preferredSize = Dimension(300, 340)
                isVisible = true
                layout = GridBagLayout()
                val gbc = GridBagConstraints()
                gbc.fill = GridBagConstraints.VERTICAL
                gbc.gridx = 0
                gbc.gridy = 0

                add(JPanel().apply {
                    preferredSize = Dimension(300, 300)
                    compressImageLabel.apply {
                        preferredSize = Dimension(300, 300)
                        verticalAlignment = SwingConstants.CENTER
                        horizontalAlignment = SwingConstants.CENTER
                    }
                    add(compressImageLabel)
                }, gbc)
                gbc.gridx = 0
                gbc.gridy = 1
                add(compressImageInfoLabel.apply {
                    preferredSize = Dimension(300, 40)
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER
                }, gbc)
            })

            //添加选择图片按钮布局
            panel.add(fileChooserBtn)
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
    }
}

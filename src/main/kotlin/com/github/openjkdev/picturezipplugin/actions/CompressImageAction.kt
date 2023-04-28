package com.github.openjkdev.picturezipplugin.actions;

import com.github.openjkdev.picturezipplugin.http.HttpUtils
import com.github.openjkdev.picturezipplugin.thread.ThreadPools
import com.github.openjkdev.picturezipplugin.utils.FileUtils
import com.github.openjkdev.picturezipplugin.utils.FormatUtils
import com.github.openjkdev.picturezipplugin.utils.MyIcons
import com.google.gson.JsonParser
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridLayout
import java.io.File
import java.net.URL
import java.util.*
import javax.swing.*

class CompressImageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        CustomDialog(e).apply {
            title = "压缩图片"
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
            val panel = JPanel()
            panel.isVisible = true
            panel.layout = GridLayout(0, 3).apply {
                hgap = 10
                vgap = 10
            }
            //列名
            panel.add(JLabel("原图片"))
            panel.add(JLabel("压缩后图片"))
            panel.add(JLabel("选择图片"))

            val fileChooserBtn = createSelectImageButton(project!!) { path, name ->
                originImageLabel.icon = createImageIcon(path, true)
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
                        compressImageLabel.icon = createImageIcon(url, false)
                        requestDownImage(it,"C:\\Users\\Administrator\\Downloads\\newPng.png"){_,msg->
                            JOptionPane.showMessageDialog(panel, msg+"-"+project?.projectFilePath)
                        }
                    }
                }
            }.apply {
                setSize(100, 40)
            }
            //原图
            panel.add(JPanel().apply {
                isVisible = true
                layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
                add(originImageLabel)
                add(originImageInfoLabel)
            })
            //压缩后图片
            panel.add(JPanel().apply {
                isVisible = true
                layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
                add(compressImageLabel)
                add(compressImageInfoLabel)
            })
            //选择图片按钮
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
        private fun requestDownImage(url:String,saveFile:String,callback: (Boolean, String) -> Unit) {
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
    }
}

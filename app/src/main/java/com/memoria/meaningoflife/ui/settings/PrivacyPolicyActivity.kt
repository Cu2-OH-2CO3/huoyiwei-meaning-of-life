package com.memoria.meaningoflife.ui.settings

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityPrivacyPolicyBinding
import com.memoria.meaningoflife.ui.BaseActivity

class PrivacyPolicyActivity : BaseActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "隐私政策"

        setupContent()
    }

    private fun setupContent() {
        binding.tvContent.text = getPrivacyPolicyText()
        binding.tvContent.movementMethod = ScrollingMovementMethod()
    }

    private fun getPrivacyPolicyText(): String {
        return """
            【活意味】隐私政策
            
            更新日期：2026年3月28日
            生效日期：2026年3月28日
            
            
            引言
            
            活意味（以下简称"本应用"）是一款帮助用户记录绘画创作、日记生活和日常决策的个人工具类应用。我们深知个人信息对您的重要性，并会尽全力保护您的个人信息安全可靠。
            
            本应用承诺：
            • 所有数据均保存在您的设备本地，不上传任何服务器
            • 不会收集任何个人身份信息
            • 不会追踪您的使用行为
            • 不会与任何第三方共享您的数据
            
            
            一、我们如何收集和使用您的信息
            
            本应用是一款纯本地应用，不会主动收集任何个人信息。您在使用过程中产生的所有数据（包括绘画记录、日记内容、菜品数据等）均存储在您的设备本地。
            
            本应用可能会申请以下权限：
            
            1. 存储权限（READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE）
               • 用途：用于保存您上传的图片（作品图片、日记图片等）
               • 说明：仅用于您主动选择的图片保存功能，不会读取其他文件
            
            2. 相机权限（CAMERA）
               • 用途：用于拍照添加图片
               • 说明：仅在您主动使用拍照功能时申请，用完即止
            
            3. 振动权限（VIBRATE）
               • 用途：用于午餐抽选时的振动反馈
               • 说明：仅用于提供更好的用户体验
            
            
            二、数据存储与安全
            
            1. 数据存储位置
               所有数据均存储在您的设备内部存储中，具体路径为：
               • 绘画作品：/storage/emulated/0/Android/data/com.memoria.meaningoflife/files/paintings/
               • 日记图片：/storage/emulated/0/Android/data/com.memoria.meaningoflife/files/diaries/images/
               • 数据库文件：/data/data/com.memoria.meaningoflife/databases/huoyiwei.db
            
            2. 数据安全
               本应用不连接任何网络服务，因此不存在数据通过网络泄露的风险。
               建议您定期备份重要数据，以防设备丢失或损坏。
            
            3. 数据删除
               删除本应用时，所有本地数据将被同时删除。您也可以在应用内手动删除单个作品或日记。
            
            
            三、您的权利
            
            由于所有数据存储在本地，您拥有对数据的完全控制权：
            
            1. 查看权：您可以随时查看所有已记录的内容
            2. 修改权：您可以随时编辑已保存的作品、日记、菜品
            3. 删除权：您可以随时删除任何记录
            4. 导出权：您可以通过设置中的"备份数据"功能导出所有数据为JSON格式
            
            
            四、第三方服务
            
            本应用不使用任何第三方分析服务、广告服务或社交登录服务。
            
            本应用使用的开源库包括：
            • Room（数据库存储）
            • Glide（图片加载）
            • Gson（JSON解析）
            • MPAndroidChart（图表展示）
            • MaterialCalendarView（日历视图）
            
            这些库仅在本地运行，不会收集或上传任何数据。
            
            
            五、儿童隐私
            
            本应用不会主动收集任何用户信息，适合所有年龄段用户使用。
            如果您是监护人，请指导儿童正确使用应用。
            
            
            六、政策更新
            
            我们可能会不时更新本隐私政策。更新后的版本将在应用内显示，并在设置页面提供查看入口。
            
            重大变更时，我们会在应用启动时通过弹窗通知您。
            
            
            七、联系我们
            
            如果您对本隐私政策有任何疑问、建议或投诉，请通过以下方式联系我们：
            
            邮箱：2383007253@qq.com
            
            
            八、适用法律
            
            本隐私政策的解释及争议解决，适用中华人民共和国法律。
            
            
            附：数据存储说明
            
            本应用创建的所有文件均存储在应用私有目录中：
            • Android 10及以上：应用沙盒目录，其他应用无法访问
            • Android 9及以下：需要存储权限，但仅访问应用自身目录
            
            致谢：
            感谢我的游戏群友们，在我开发的路上提供了致命诱惑的作用。
            感谢kotimatka提供了很多的反馈和命名的灵感！
            感谢咚麻和jyydyxrsg帮忙测试了应用在不同手机上的兼容性！
            
            
            感谢老大使用活意味喵！希望老大在地球OL打猎时也能寻找到生活的意义喵！
            
            —— 铜绿
            
        """.trimIndent()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
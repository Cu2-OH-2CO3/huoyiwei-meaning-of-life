# 🎨 活意味 - 记录创作，品味生活

活意味是一款帮助用户记录绘画创作、日记生活和日常决策的个人工具类 Android 应用。

## ✨ 功能特点

### 🎨 绘画记录
- 记录绘画作品，添加创作节点
- 支持时长记录（单节点/累计模式）
- 作品标签分类（草稿/练习/成图）
- 设定绘画目标，追踪进度

### 📔 日记本
- 记录每日心情，支持图片添加
- 自定义标签筛选
- 高频词统计、篇数统计、字数统计
- 时间范围筛选（本周/本月/全部）

### 🍱 午餐抽选
- 随机抽选午餐，解决选择困难
- 管理菜品菜单（菜系、辣度）
- 滚动抽选动画，震动反馈
- 抽选历史记录

### 📅 日历视图
- 三种查看模式（全部/绘画/日记）
- 主题色方块标记完成情况
- 左右滑动切换月份
- 年份滚动轴

### 🎨 个性化设置
- 四种主题色（橙色/绿色/蓝色/紫色）
- 深色模式
- 每日一言自定义
- 数据备份导出

## 📱 下载

### 正式版
- [GitHub Releases](https://github.com/你的用户名/huoyiwei/releases)

## 🛠 技术栈

- **开发语言**: Kotlin
- **最低 SDK**: API 24 (Android 7.0)
- **目标 SDK**: API 34
- **架构**: MVVM
- **数据库**: Room
- **图片加载**: Glide
- **图表**: MPAndroidChart
- **日历**: MaterialCalendarView

## 📁 项目结构
app/src/main/java/com/memoria/meaningoflife/
├── data/
│ ├── database/ # Room 数据库
│ │ ├── painting/ # 绘画模块
│ │ ├── diary/ # 日记模块
│ │ ├── lunch/ # 午餐模块
│ │ └── checkin/ # 签到模块
│ └── repository/ # 数据仓库
├── ui/
│ ├── home/ # 首页
│ ├── calendar/ # 日历
│ ├── settings/ # 设置
│ ├── painting/ # 绘画记录
│ ├── diary/ # 日记本
│ └── lunch/ # 午餐抽选
└── utils/ # 工具类


## 🚀 开发环境

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 11 或更高版本
- Gradle 8.0+


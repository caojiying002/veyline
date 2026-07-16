# Veyline

基于 Jetpack Compose 的信息流客户端，是一个以生产级标准构建的 **Android 架构展示项目**。所有数据来自内置 Mock 数据源，克隆后可直接编译运行，无需后端服务或任何额外配置。

> 项目中的架构设计与工程封装，来自我在长期实际客户端开发中沉淀的实践，此仓库是这些实践的公开载体。

## 功能特性

- 信息流列表 → 详情页 → 图片查看器的完整浏览链路
- 登录 / 注册，含 Token 管理与全局登录态分发
- 多 Tab 主容器，Tab 切换保持滚动位置等 UI 状态
- 暗色模式，基于语义化色彩系统
- 统一处理加载中、空态、业务错误、网络错误、下拉刷新与分页加载

## 截图

<!-- TODO: 迁移完成后补充亮色/暗色模式各 2-3 张截图 -->

## 架构

单 Activity + Compose 导航，整体遵循单向数据流（UDF）：

```
UI (Compose) → ViewModel (UiState + Effect) → Repository → DataSource (Mock / Remote 可切换)
```

- **状态管理**：每个页面收敛为单一 `UiState`，一次性事件通过 `Effect` 通道下发，避免多状态字段互相打架
- **导航**：Navigation Compose + Kotlin Serialization 实现类型化路由，路由参数编译期安全
- **网络与错误**：`ApiResult` 四态模型区分成功、业务错误、网络错误与未知异常；`apiCall` 统一异常捕获与错误模型转换
- **列表**：通用列表容器统一封装加载、空态、错误、刷新与底部加载状态，各信息流页面零重复代码接入
- **状态保持**：`SaveableStateHolder` 保持 Tab 内滚动位置、Pager 页码等关键 UI 状态
- **主题**：`AppColors` 语义色层 + 基础组件封装，暗色模式零散色值零硬编码

<!-- TODO: 迁移完成后补一张模块分层图 -->

## Mock 数据源

网络层通过 OkHttp Interceptor 从本地 JSON 返回响应，Retrofit / Moshi / Repository 链路与真实环境完全一致：

- 内置模拟延迟，可复现慢网络下的加载体验
- 可按接口注入业务错误 / 网络错误，用于演示完整的错误处理链路
- `Repository` 抽象保证随时可替换为真实远端数据源

## 构建运行

```
git clone https://github.com/caojiying002/veyline.git
```

使用 Android Studio 打开，直接运行 `app` 模块即可。

<!-- TODO: 确认后填写：最低 AS 版本 / JDK 版本 / minSdk-targetSdk -->

## Roadmap

- [ ] 核心浏览链路迁移（信息流 / 详情 / 图片查看器）
- [ ] 登录注册与 Token 管理
- [ ] 通用列表容器与错误处理演示入口
- [ ] 截图与架构图
- [ ] 单元测试补充

## License

MIT

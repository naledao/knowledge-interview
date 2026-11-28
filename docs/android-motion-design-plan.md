# 安卓端动效与交互灵性化设计规划

> 本文档面向后续负责落地实现的开发 Agent，基于当前 `Android/` 原生壳层（Java 17 + Android Framework，无重型第三方 UI 库）与 `frontend/` 内置 Web 资源（`KnowledgeInterviewAndroid/`）的实际架构，系统规划一套**轻盈、跟手、克制、有呼吸感**的安卓端动效体系。

---

## 1. 设计理念与核心原则

### 1.1 什么是“有灵性”的动态体验
不是堆砌花哨特效，而是让每一次启动、切换、点击、加载、反馈都具备**物理直觉**和**情绪温度**：
- **有来有去**：页面、弹窗、菜单的出现和消失都沿自然运动方向过渡，不生硬闪现/消失。
- **即时跟手**：手指按下即有微缩放与波纹回馈，关键操作配合轻触觉反馈（Haptic），消除“网页壳子”的迟钝感。
- **状态可见**：加载、进度、刷新、错误重试都有平滑的过渡动画，避免白屏或生硬跳变。
- **克制高效**：高频操作动效控制在 `150ms–260ms`，不阻塞用户连续点击；尊重系统「移除动画 / 动画缩放」设置。

### 1.2 技术与架构约束
1. **零重型依赖**：`Android/app/build.gradle` 当前仅依赖 `androidx.webkit:webkit:1.11.0`（`minSdk 24 / targetSdk 34`）。所有原生动效均使用 Android SDK 内置能力（`ViewPropertyAnimator`、`ObjectAnimator`、`ValueAnimator`、`PathInterpolator`、`RippleDrawable`、`res/anim/*.xml`、`HapticFeedbackConstants`）实现，不引入 Lottie/Compose/Material3 等重型依赖。
2. **双层协同**：安卓端体验由 **原生层（`Android/`）** 与 **WebView 内前端层（`frontend/`，打包至 `assets/www/`）** 共同构成，两层动效需统一时间曲线与风格。
3. **保护既有核心机制**：
   - 不可破坏 `MainActivity ↔ AiActivity` 的 `FLAG_ACTIVITY_REORDER_TO_FRONT | FLAG_ACTIVITY_SINGLE_TOP` 双 Activity 保活。
   - 不可破坏 `App.vue` 中 `isNativeAndroid` 下的 `<KeepAlive>` 与 `router/index.ts` 的滚动位置记忆恢复。
   - 不可向 WebView 注入不安全的 JS Bridge（沿用现有 `knowledgeinterview://` 受限协议与原生自身生命周期事件）。

---

## 2. 统一动效语言（Motion Tokens）

原生层（建议新增工具类 `MotionHelper.java`）与前端层（`frontend/src/style.css`）共用同一套节奏与缓动曲线：

| 层级 | 场景 | 时长 | 曲线（CSS / Android `PathInterpolator`） |
| --- | --- | --- | --- |
| **微交互** | 按钮/卡片按压、开关切换、标签高亮 | `120ms–160ms` | `cubic-bezier(0.2, 0, 0, 1)` |
| **入场/展开** | 弹窗弹出、菜单展开、卡片渐入、答案展开 | `220ms–280ms` | 缓出带轻回弹：`cubic-bezier(0.22, 1, 0.36, 1)` 或 `OvershootInterpolator(0.8f)` |
| **退场/收起** | 弹窗关闭、遮罩淡出、骨架屏消退 | `150ms–200ms` | 快速缓入：`cubic-bezier(0.4, 0, 1, 1)` |
| **页面转场** | `MainActivity ↔ AiActivity`、WebView 路由推进/返回 | `240ms–300ms` | 标准强调缓出：`cubic-bezier(0.2, 0.8, 0.2, 1)` |
| **循环呼吸** | 加载骨架 Shimmer、刷新旋转、状态呼吸点 | `1200ms–1600ms` | `ease-in-out` / `linear` |

---

## 3. 原生层动效规划（`Android/`）

### 3.1 统一动效工具类（新增 `MotionHelper.java`）
在 `com.knowledgeinterview.android` 下新增轻量工具类 `MotionHelper`，集中封装：
- **系统动画开关检测**：读取 `Settings.Global.ANIMATOR_DURATION_SCALE`（为 `0` 时自动降级为瞬时切换）。
- **按压微回弹绑定 `attachPressScale(View v)`**：`ACTION_DOWN` 时 `scaleX/scaleY -> 0.96f`（`100ms`），`ACTION_UP / CANCEL` 时弹性回弹至 `1.0f`（`160ms`），并配合 `RippleDrawable` 波纹与轻触觉反馈 `HapticFeedbackConstants.KEYBOARD_TAP`。
- **弹窗入场/退场动画 `animateDialogEnter(View card)` / `animateDialogExit(Dialog d, View card)`**：卡片从 `alpha: 0, scale: 0.94, translationY: +16dp` 缓出至 `1, 1, 0`（`240ms`）；关闭时缩小淡出（`150ms`）后再 `dismiss()`。
- **表单校验失败轻抖动 `shake(View v)`**：输入非法地址/空密码时，执行 `0 -> -8dp -> 8dp -> -5dp -> 5dp -> 0` 的 `280ms` 水平轻颤 + 轻震动反馈。

### 3.2 启动首屏平滑过渡（`MainActivity.java`）
- **现状问题**：`onCreate` 直接 `setContentView(webView)`，WebView 首次渲染前存在短暂白屏跳变。
- **改造方案**：
  1. 将根容器改为 `FrameLayout`，底层为 `webView`，顶层覆盖一层轻量原生品牌启动遮罩（白色背景 + 居中应用图标 + 轻呼吸脉冲圈）。
  2. 当 `onPageFinished` 首次触发（或超过 `1200ms` 兜底超时）时，启动遮罩执行 `alpha: 1 -> 0` + `scale: 1 -> 1.04`（`240ms`）淡出并从父容器移除，实现“冷启动无缝融入首页”。

### 3.3 `MainActivity ↔ AiActivity` 切换转场（`res/anim/` + 两端 Activity）
- **现状问题**：在知识库与 AI 助手之间切换时画面生硬硬切，缺少空间层次感。
- **改造方案**：
  1. 新增 4 个轻量 XML 补间动画资源（`res/anim/`）：
     - `slide_in_right_fade.xml`（从右侧 `18%` 滑入并淡入，`260ms`）
     - `slide_out_left_scale.xml`（轻微左移 `6%` 并缩放至 `0.97` + 微暗，`260ms`）
     - `slide_in_left_scale.xml`（返回时从 `0.97` 恢复至 `1.0` 并右移复位，`240ms`）
     - `slide_out_right_fade.xml`（AI 页向右滑出并淡出，`240ms`）
  2. `MainActivity` 打开 `AiActivity` 时调用转场动画（Android 14+ 兼容 `overrideActivityTransition`，低版本使用 `overridePendingTransition`）；`AiActivity.returnToKnowledge()` 返回时调用反向退出动画。
  3. 仅做转场视觉过渡，保持 `FLAG_ACTIVITY_REORDER_TO_FRONT` 保活逻辑完全不变。

### 3.4 `AiActivity` 顶栏、进度条与错误态动效（`AiActivity.java`）
- **丝滑进度条**：为顶部 `3dp` `ProgressBar` 增加 `ObjectAnimator` 平滑插值推进（而非直接 `setProgress` 跳帧）；进度达到 `100` 后停留 `120ms` 再执行 `alpha -> 0` 淡出并隐藏。
- **顶栏按钮灵动反馈**：`‹ 题库` 和 `刷新` 按钮接入 `MotionHelper.attachPressScale` + `RippleDrawable`；点击「刷新」时给按钮图标/文字加短暂旋转或轻弹反馈。
- **连接状态呼吸指示点**：在顶栏标题「AI 助手」旁增加一个 `6dp` 圆点，加载/重连时呈柔和蓝紫色呼吸闪烁，就绪后常亮柔和绿点，连接失败变为浅红点。
- **错误面板过渡**：`errorPanel` 显示时执行 `alpha: 0 -> 1` + `translationY: 12dp -> 0`（`220ms`）平滑浮现，避免突然闪白覆盖。

### 3.5 原生弹窗统一灵性化升级（`MainActivity.java` / `AiActivity.java` / `UpdateDialogs.java`）
覆盖全部 4 类原生弹窗（服务器地址设置、Trojan 代理配置、AI 助手登录、应用更新与下载进度）：
- 统一使用 `MotionHelper.animateDialogEnter` / `animateDialogExit` 实现带微缩放与上浮的入场和退场。
- 所有主/次按钮接入 `RippleDrawable` 与 `attachPressScale`。
- 表单提交校验失败时触发 `MotionHelper.shake(input)`，替代生硬的纯 Toast 提示。
- `UpdateDialogs` 下载进度条采用平滑插值动画，下载完成切换「立即安装」状态时带轻弹反馈。

---

## 4. WebView 内移动端动效规划（`frontend/`，同步构建至 `Android/app/src/main/assets/www/`）

为了避免“原生壳子很灵动，里面页面依然僵硬”，建议在前端针对移动端 / `isNativeAndroid` 配套落地以下高感知微动效：

### 4.1 路由切换过渡（`frontend/src/App.vue` + `frontend/src/router/index.ts`）
- **现状问题**：`App.vue` 中 `isNativeAndroid` 的 `<RouterView><KeepAlive>` 无任何 `<Transition>`，页面切换为瞬间硬切。
- **改造方案**：
  1. 在 `router/index.ts` 中记录导航方向（前进 `forward` / 返回 `back`，可通过 `history.state?.position` 差值判断）。
  2. 在 `App.vue` 中使用 `<Transition :name="routeTransitionName" mode="out-in">` 包裹 `<KeepAlive>` 内的动态组件（注意：`mode="out-in"` 与单根节点组件配合，动画时长控制在 `180ms–220ms` 以保持爽快不拖沓）：
     - 前进：新页面 `translateX(14px) -> 0` + `opacity: 0 -> 1`；
     - 返回：新页面 `translateX(-12px) -> 0` + `opacity: 0 -> 1`。
  3. 动画期间仅使用 `transform` 和 `opacity`（GPU 合成层），确保不干扰 `router/index.ts` 的 `scrollPositions` 恢复。

### 4.2 触控按压反馈（全站可点击卡片与按钮）
- 在 `style.css` 及各视图中，为所有可点击卡片（`.category-card`、`.recent-item`、`.topic-card`、`.question-card`、`.result-card`、`.nav-card`）与核心按钮增加移动端触控按压态：
  - `:active` 时 `transform: scale(0.982)`（`100ms`），松手回弹；
  - 统一设置 `-webkit-tap-highlight-color: transparent`，消除 Android WebView 默认的生硬灰色点击高亮块，改用干净的按压缩放 + 浅蓝底色反馈。

### 4.3 列表错峰入场与轻量骨架屏（首屏与搜索结果）
- **轻量骨架屏（Shimmer）**：将首页、分类页、主题页、问题页、搜索页的纯文字/单转圈加载态升级为带微光泽扫过（`background-position` 渐变流光）的卡片骨架，减少等待焦虑。
- **首屏卡片错峰浮现**：数据加载完成后，首屏可见的前 6–8 张卡片执行极轻量的淡入上移（`translateY(8px) -> 0`, `opacity: 0 -> 1`, `220ms`，每项延迟 `25ms` 递增，上限 `150ms`），仅在首次加载时触发，从 `<KeepAlive>` 缓存返回时不重复播放。

### 4.4 高频交互细节微动效
- **移动端抽屉菜单（`App.vue` `.mobile-nav`）**：菜单展开使用带微弹性的 `translateY(-8px) + opacity` 过渡，菜单项依次轻错峰出现；汉堡图标 `Menu ↔ X` 增加 `90deg` 轻旋转缩放切换。
- **刷新按钮（`App.vue` `.page-refresh-button`）**：已有旋转动画，补充点击轻缩回弹反馈。
- **问题详情页（`QuestionDetailView.vue`）**：
  - 答案展开/收起增加平滑高度与透明度过渡（如 CSS Grid `grid-template-rows: 0fr -> 1fr` + `opacity`），不再瞬间闪现；
  - 顶部增加 `2px` 细阅读进度条（随页面滚动平滑更新 `scaleX`），并在长答案下滑后浮现一个小巧的「回到顶部」悬浮圆钮（淡入上浮出现）。
- **搜索模式切换与筛选标签（`SearchModeSelect.vue` & `SearchView.vue`）**：下拉面板带 `scale(0.96) + translateY(-4px)` 弹入动画；筛选标签激活时平滑过渡底色与文字色。

---

## 5. 性能、兼容性与可访问性红线

1. **只动合成属性**：所有高频动画优先使用 `transform`（`translate`/`scale`/`rotate`）和 `opacity`，避免在动画帧中频繁触发 `width`/`height`/`top`/`margin` 重排。
2. **尊重减弱动态设置**：
   - Web 层：所有新增动画必须包裹在 `@media (prefers-reduced-motion: reduce)` 降级规则中。
   - Android 原生层：`MotionHelper` 必须检查 `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`，关闭系统动画时直接应用终态。
3. **不阻塞操作**：点击动画、列表错峰动画均不可拦截指针事件；用户快速连点时应能立即打断旧动画并响应新操作（`view.animate().cancel()`）。
4. **前端资源同步**：修改 `frontend/` 后需重新构建并将 `frontend/dist` 同步到 `Android/app/src/main/assets/www/`（按项目既有打包脚本/流程执行）。

---

## 6. 建议分阶段实施路径与验收清单

### 实施顺序
1. **Phase 1（原生动效底座与转场）**：
   - 新增 `MotionHelper.java` 与 `res/anim/` 4 个转场动画文件；
   - 落地 `MainActivity ↔ AiActivity` 切换动画与 `MainActivity` 启动首屏淡出遮罩。
2. **Phase 2（原生控件与弹窗灵性化）**：
   - 升级 `AiActivity` 平滑进度条、状态呼吸点、错误面板过渡与顶栏按压反馈；
   - 统一改造 4 类原生弹窗的入场/退场、按钮按压回弹与输入校验轻抖动。
3. **Phase 3（WebView 内微动效与构建同步）**：
   - 落地 `App.vue` 路由方向过渡、全站移动端去灰块 + 按压缩放、抽屉菜单动效；
   - 落地骨架屏 Shimmer、答案展开过渡、阅读进度条与回到顶部浮钮；
   - 重新构建前端并同步至 `Android/app/src/main/assets/www/`。

### 验收清单
- [ ] `Android` 模块执行 Gradle 构建通过（无编译警告/错误），`frontend` 执行 `npm run build` 通过。
- [ ] 真机/模拟器实测冷启动 `MainActivity` 无明显白屏闪烁，平滑过渡进入首页。
- [ ] `MainActivity ↔ AiActivity` 来回切换多次，转场流畅且双方 WebView 状态与滚动位置均保持不重载。
- [ ] 4 类原生弹窗弹出/关闭顺滑，非法输入时抖动提示清晰；连点按钮无卡死或动画错乱。
- [ ] WebView 内点击卡片与按钮无默认灰块、按压跟手，前进/返回页面过渡方向正确且 `<KeepAlive>` 返回后滚动位置正常恢复。
- [ ] 开启系统「移除动画」后，所有原生与 Web 动画自动静默降级，无白屏或元素停留在 `alpha=0` 的卡死状态。
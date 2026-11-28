# 前端页面紧凑化布局设计规划

> 本文档面向后续负责落地实现的开发 Agent，基于当前 `frontend/` 实际代码梳理页面布局松散的根因，给出统一的全局间距体系、逐页面改造方案、兼容性约束及验收标准。

---

## 1. 优化目标与设计原则

### 1.1 核心目标
- **消除双重留白**：收口全局容器 `.site-main` 与各子页面容器重复叠加的边距和内边距。
- **提升首屏信息密度**：压缩过高的 Hero 横幅、装饰图形和区块间距，让桌面端与移动端首屏能直接看到核心列表或正文内容。
- **统一间距标尺与样式规范**：通过 CSS 变量统一顶栏高度、区块间距、卡片内边距与圆角，并合并各页面 `<style scoped>` 中历史叠加的重复覆盖规则。

### 1.2 设计原则
1. **紧凑而不拥挤**：削减无效大空白与过大的 `min-height`，但保留清晰的内容分组层次和阅读呼吸感（尤其是问题详情页正文）。
2. **不牺牲可操作性**：按钮、输入框、筛选标签等可交互元素在移动端保持 ≥ 36px 的点击热区，保留 `focus-visible` 与 `prefers-reduced-motion` 支持。
3. **纯展示层改造**：原则上不改动业务逻辑、路由参数、接口请求与状态管理；如为紧凑排版需要微调模板结构，须保持原有功能与可访问性属性完整。

---

## 2. 现状问题诊断

经排查 `frontend/src/style.css` 及各视图文件，当前布局不紧凑主要由以下四类原因叠加造成：

1. **全局容器与子页面双重留白**
   - `style.css` 中 `.site-main` 已设置 `width: min(100% - 40px, 1100px); margin: 36px auto;`。
   - 多个子页面又各自叠加了较大的外层 `padding`：`CategoryDetailView`、 `TopicDetailView`、`QuestionDetailView` 均为 `padding: 32px 24px 88px`（顶部实际空白达 `68px`，左右达 `44px`）；`HomeView` 为 `padding: 16px 4px 76px`；`SearchView` 为 `padding: 12px 12px 90px`。
2. **Hero 区与装饰元素过度占高**
   - `HomeView` 的 `.home-hero` 内边距达 `44px 48px 48px`。
   - `CategoryDetailView` 与 `TopicDetailView` 右侧装饰圆分别占 `185×185px`、`176×176px`，叠加 `48×48px` 图标与多段 `margin`。
   - `SearchView` 的 `.search-hero` 既包含标题、副标题、大搜索框与热搜词，又设置了 `position: sticky; top: 74px`（移动端 `62px`），滚动结果时占据过多视口高度。
3. **区块间距与卡片最小高度偏大**
   - 区块间距普遍在 `47px–56px`（如 `.home-section`、`topics-section`、`questions-section`）。
   - 卡片 `min-height` 偏高：首页分类卡 `162px`（移动端 `147px`）、分类页主题卡 `225px`（且为 2 列）、主题页问题卡 `106px`（编号块 `52×52px`）、问题页前后导航卡 `117px`、状态/空态面板 `padding: 75px 24px`。
4. **历史覆盖样式叠加**
   - `HomeView.vue`、`CategoryDetailView.vue`、`TopicDetailView.vue`、`QuestionDetailView.vue`、`SearchView.vue` 的 `<style scoped>` 末尾存在多段补丁式覆盖，同一选择器出现 2–3 次，导致间距不统一且修改易被后置规则覆盖。

---

## 3. 全局布局与 Spacing Tokens 规范

在 `frontend/src/style.css` 的 `:root` 中新增统一的布局变量，供全局与各页面复用：

```css
:root {
  --header-height: 60px;
  --page-max-width: 1120px;
  --page-gutter-x: 20px;
  --page-gap-top: 20px;
  --page-gap-bottom: 44px;

  --section-gap: 24px;
  --section-heading-gap: 12px;
  --breadcrumb-gap: 14px;

  --hero-padding-y: 20px;
  --hero-padding-x: 24px;
  --hero-radius: 16px;

  --card-padding-y: 14px;
  --card-padding-x: 16px;
  --card-radius: 12px;
  --grid-gap: 12px;
}

@media (max-width: 760px) {
  :root {
    --header-height: 54px;
    --page-gutter-x: 12px;
    --page-gap-top: 14px;
    --page-gap-bottom: 32px;

    --section-gap: 18px;
    --section-heading-gap: 10px;
    --breadcrumb-gap: 10px;

    --hero-padding-y: 16px;
    --hero-padding-x: 16px;
    --card-padding-y: 12px;
    --card-padding-x: 14px;
    --grid-gap: 10px;
  }
}
```

### 3.1 全局壳层（`frontend/src/style.css` & `frontend/src/App.vue`）

| 选择器 | 现状 | 目标建议 |
| --- | --- | --- |
| `.header-inner` | 桌面 `height: 74px; gap: 30px`，移动 `height: 62px` | 统一使用 `height: var(--header-height)`（桌面 `60px` / 移动 `54px`），桌面 `gap: 20px` |
| `.brand` / `.brand-mark` | `20px` / `37×37px` | 桌面 `18px` / `32×32px`，移动 `17px` / `30×30px` |
| `.header-search` | `height: 39px` | `height: 36px`，按钮与输入框同步微调 |
| `.site-main` | `width: min(100% - 40px, 1100px); margin: 36px auto;` | `width: min(100% - var(--page-gutter-x) * 2, var(--page-max-width)); margin: var(--page-gap-top) auto var(--page-gap-bottom);` |
| 子页面根容器 | 各自附加 `padding: 12px–32px 4px–24px 76px–90px` | **统一归零外层留白**：`padding: 0`（或移除重复的 `max-width` / `padding`），全站外边距完全由 `.site-main` 控制 |
| `.mobile-nav` | 项高 `46px`，`padding: 14px ... 22px` | 项高 `42px`，上下内边距收敛为 `10px ... 14px` |

---

## 4. 逐页面改造方案

### 4.1 首页（`frontend/src/views/HomeView.vue`）

| 区域 / 选择器 | 现状 | 目标改造 |
| --- | --- | --- |
| `.home-page` | `padding: 16px 4px 76px` | `padding: 0` |
| `.home-hero` | `padding: 44px 48px 48px; border-radius: 22px` | `padding: 24px 28px; border-radius: var(--hero-radius)`（移动 `18px 16px`）；`.home-kicker` 下边距 `6px`；`h1` 收敛至 `clamp(22px, 3vw, 30px)`；`.home-subtitle` `margin: 8px 0 16px` |
| `.home-search` + `.home-random` | 搜索框与“随机一题”分行且间距偏大 | `.home-search` `padding: 5px 5px 5px 14px`；按钮 `padding: 9px 18px`；桌面端可将“随机一题”与搜索框同排或保持 `margin-top: 10px` 并压缩按钮为 `padding: 7px 14px; font-size: 13px` |
| `.home-section` / `.recent-section` | `margin-top: 47px` / `48px`，标题下边距 `20px` | `margin-top: var(--section-gap)`（`24px`），标题下边距 `var(--section-heading-gap)`（`12px`），`h2` 字号 `18px–19px` |
| `.category-grid` / `.category-card` | 3 列，`min-height: 162px; padding: 22px`，图标 `44×44px` | 桌面维持 3 列（宽屏 ≥ 1040px 可选 4 列或保持 3 列），**移除 `min-height: 162px`**（改为 `auto` 或 `min-height: 96px`），`padding: 14px 16px; gap: 12px`；图标缩至 `34×34px`，`.category-card-top` 下边距 `10px`；移动端 `min-height: auto; padding: 12px` |
| `.recent-item` | `min-height: 65px; padding: 16px 22px; gap: 16px` | `min-height: 46px; padding: 10px 14px; gap: 12px`；图标缩至 `30×30px` |

### 4.2 分类详情页（`frontend/src/views/CategoryDetailView.vue`）

| 区域 / 选择器 | 现状 | 目标改造 |
| --- | --- | --- |
| `.category-page` / `.breadcrumbs` | `padding: 32px 24px 88px` / `margin-bottom: 28px` | `.category-page { padding: 0 }`；`.breadcrumbs { margin-bottom: var(--breadcrumb-gap); font-size: 13px; gap: 8px }` |
| `.category-hero` | 初始 `padding: 52px 56px`，装饰圆 `185×185px`，描述 `margin: 22px 0 28px` | `padding: var(--hero-padding-y) var(--hero-padding-x)`（`20px 24px`）；`h1` 收敛至 `clamp(22px, 2.8vw, 28px)`；`.category-description` `margin: 8px 0 14px; font-size: 14px`；**缩小右侧装饰圆至 `88–104px`（或在 ≤ 760px 隐藏）**，避免撑高 Hero |
| `.topics-section` / `.section-heading` | `padding-top: 56px` / `margin-bottom: 26px` | `padding-top: var(--section-gap)`；`.section-heading` `margin-bottom: var(--section-heading-gap)`；`h2` 字号 `18px–20px` |
| `.topic-grid` / `.topic-card` | 2 列，`gap: 20px`，`min-height: 225px; padding: 25px 27px 21px` | 桌面端改为 **`repeat(3, minmax(0, 1fr))`**（≤ 900px 2 列，≤ 560px 1 列），`gap: var(--grid-gap)`；**移除 `min-height: 225px`**，改为 `padding: 14px 16px`；`h3` `margin: 10px 0 6px; font-size: 16px`；`.topic-description` 改为 `-webkit-line-clamp: 2; margin-bottom: 12px; font-size: 13px; line-height: 1.55`；`.topic-card-footer` `padding-top: 10px` |
| `.empty-state` / `.state-panel` | `padding: 75px 24px` | `padding: 36px 20px` |

### 4.3 主题详情页（`frontend/src/views/TopicDetailView.vue`）

| 区域 / 选择器 | 现状 | 目标改造 |
| --- | --- | --- |
| `.topic-page` / `.breadcrumbs` | `padding: 32px 24px 88px` / `margin-bottom: 28px` | `.topic-page { padding: 0 }`；`.breadcrumbs { margin-bottom: var(--breadcrumb-gap); font-size: 13px; gap: 8px }` |
| `.topic-hero` | `padding: 30px 36px`，装饰圆 `176×176px`，上方 `.hero-icon` `48×48px`（`margin-bottom: 19px`） | `padding: var(--hero-padding-y) var(--hero-padding-x)`；将 `.hero-icon` 缩至 `36×36px` 并与标题横向对齐（或减至 `margin-bottom: 10px`）；`h1` `clamp(22px, 2.8vw, 28px)`；`.hero-description` `margin: 8px 0 12px`；右侧装饰圆缩至 `84–96px` 或移除 |
| `.questions-section` / `.section-heading` | `padding-top: 54px` / `margin-bottom: 26px` | `padding-top: var(--section-gap)`；`.section-heading` `margin-bottom: var(--section-heading-gap)`；`h2` 字号 `18px–20px` |
| `.question-list` / `.question-card` | `gap: 14px`，`min-height: 106px; padding: 22px 26px; gap: 21px`，序号 `52×52px` | `gap: 8px`；**移除 `min-height: 106px`**，改为 `padding: 12px 16px; gap: 12px; border-radius: 12px`；`.question-number` 缩至 `36×36px; font-size: 13px; border-radius: 9px`；`.question-main` `gap: 4px`，标题 `font-size: 15px; line-height: 1.45`；`.question-arrow` 缩至 `28×28px` |

### 4.4 问题详情页（`frontend/src/views/QuestionDetailView.vue`）

| 区域 / 选择器 | 现状 | 目标改造 |
| --- | --- | --- |
| `.question-page` / `.breadcrumbs` | `padding: 32px 24px 88px` / `margin-bottom: 28px` | `.question-page { padding: 0 }`；`.breadcrumbs { margin-bottom: var(--breadcrumb-gap); font-size: 13px; gap: 8px }` |
| `.question-hero` | `padding: 28px 36px`，`h1` `margin: 0 0 20px`，`.hero-footer` `padding-top: 20px` | `padding: 18px 22px; border-radius: var(--hero-radius)`（移动 `14px 16px`）；`.hero-icon` 缩至 `36×36px` 或与 `.progress-pill` 同行紧凑展示；`h1` `margin: 10px 0 12px; font-size: clamp(20px, 2.6vw, 26px); line-height: 1.4`；`.hero-footer` `padding-top: 12px` |
| `.answer-section` / `.answer-card` | `margin-top: 30px`；`.answer-heading` `margin-bottom: 20px`；`.answer-card` `padding: 34px 38px; min-height: 170px` | `margin-top: 18px`；`.answer-heading` `margin-bottom: 10px`，`h2` 字号 `18px–19px`，`.answer-toggle` `padding: 7px 12px; font-size: 13px`；`.answer-card` 收敛至 `padding: 20px 24px; min-height: 100px; border-radius: 14px`（移动 `16px`），**保持 `.answer-card.answer-card-html` 无边框与零内边距**；`.answer-text` 行高保持 `1.75–1.8` 以兼顾长文可读性 |
| `.question-navigation` / `.nav-card` | `margin-top: 38px; gap: 18px`；`.nav-card` `min-height: 117px; padding: 24px 27px; gap: 13px` | `margin-top: 20px; gap: 12px`；**移除 `min-height: 117px`**，改为 `padding: 14px 16px; gap: 6px; border-radius: 12px` |
| `.random-next` / `.back-row` | `margin-top: 22px` / `margin-top: 32px` | 合并间距至 `margin-top: 14px` / `16px`，`.random-next-button` `padding: 9px 18px; font-size: 14px` |

### 4.5 搜索页（`frontend/src/views/SearchView.vue`）

搜索页是目前移动端与小屏桌面端**垂直空间挤压最严重**的页面，重点在于精简 `.search-hero` 及其 `sticky` 占用：

| 区域 / 选择器 | 现状 | 目标改造 |
| --- | --- | --- |
| `.search-page` / `.breadcrumbs` | `padding: 12px 12px 90px` / `margin-bottom: 27px` | `.search-page { padding: 0 }`；`.breadcrumbs { margin-bottom: var(--breadcrumb-gap) }` |
| `.search-hero` | `padding: 52px 56px 46px`（移动 `24px 18px`），含标题、副标题、搜索框、热搜词，且 `position: sticky; top: 74px`（移动 `62px`） | 1. 内边距压缩至 `16px 20px`（移动 `12px 14px`），`border-radius: 14px`，隐藏大装饰伪元素 `::after` 或大幅缩小；<br>2. `top` 改用 `var(--header-height)`，与全局顶栏严格同步；<br>3. 精简标题区（`h1` 收敛至 `20px–22px`），去掉或缩短冗余副标题留白；`.search-form` 高度收敛至 `min-height: 44px; padding: 4px 5px 4px 12px`；`.suggestions` `margin-top: 10px; gap: 6px`，热搜标签 `padding: 4px 10px`；<br>4. **移动端（≤ 640px）建议取消整个 `.search-hero` 的 sticky（改为 `position: static`）**，或仅让搜索栏本身吸顶，避免热搜和标题长期占去半个屏幕 |
| `.search-content` / `.result-heading` | `padding-top: 38px` / `margin-bottom: 28px` | `padding-top: 18px`；`.result-heading` `margin-bottom: 12px`，`h2` 字号 `18px` |
| `.filter-bar` / `.filter-button` | `padding-bottom: 17px; margin-bottom: 12px`；按钮 `padding: 8px 13px` | `padding-bottom: 10px; margin-bottom: 10px; gap: 8px`；按钮 `padding: 6px 11px; font-size: 13px` |
| `.result-list` / `.result-card` | `gap: 10px`，`padding: 18px 22px`（移动 `16px`） | `gap: 8px`，`padding: 12px 16px`（移动 `12px`）；`.result-topline` `margin-bottom: 4px`；`h3` `margin-bottom: 4px; font-size: 15px`；`.result-description` `font-size: 13px; line-height: 1.55` |

### 4.6 其他页面与公共组件

- **`frontend/src/views/ApiKeyView.vue`**：`.key-card` 内边距由偏大值收敛至 `24px 28px`（移动 `18px 16px`）；`.key-icon` 由 `60×60px` 缩至 `44×44px`（`margin-bottom: 12px`）；`.submit-button` 高度 `44px`，`.key-note` `margin-top: 14px; padding: 10px 12px`。
- **`frontend/src/views/NotFoundView.vue`**：收敛外层留白与卡片内边距，确保首屏无须滚动即可看到全部引导入口。
- **`frontend/src/components/ListPagination.vue`**：`.list-pagination` 由 `margin: 28px 0 6px; gap: 16px` 收敛为 `margin: 16px 0 4px; gap: 10px; font-size: 13px`；按钮 `padding: 6px 12px`。
- **`frontend/src/components/MarkdownContent.vue`**：正文排版保持良好可读性，无需过度压缩，仅按需微调代码块与表格上下 `margin`（如 `1em → 0.8em`）。

---

## 5. 样式清理与兼容性约束

1. **清理重复 CSS 覆盖段**：改造每个 `.vue` 文件时，先合并该文件 `<style scoped>` 末尾反复重写同一选择器的旧规则，保证每个选择器仅保留一份清晰定义（基础样式 + 响应式媒体查询）。
2. **联动 `sticky` 偏移量**：修改 `style.css` 顶栏高度后，必须确保 `SearchView.vue` 的 `top` 使用 `var(--header-height)`，避免出现吸顶缝隙或遮挡顶栏下边框。
3. **保护 Android WebView 特性**：
   - 不可影响 `App.vue` 中 `isNativeAndroid` 条件下的导航项与 `<KeepAlive>` 缓存结构。
   - 不可破坏 `router/index.ts` 中针对 `KnowledgeInterviewAndroid/` 的滚动位置记录与恢复行为（尽量避免把整个页面改成内部独立滚动容器）。
4. **保护 HTML 答案渲染**：`QuestionDetailView.vue` 中的 `.answer-card.answer-card-html` 需继续保持 `padding: 0; border: 0; background: transparent`，以免破坏富文本 HTML 答案自有布局。

---

## 6. 建议实施步骤与验收清单

### 实施顺序
1. **Phase 1（全局底座）**：更新 `frontend/src/style.css` 定义 Spacing Tokens，收敛顶栏高度与 `.site-main` 边距；同步 `SearchView.vue` 吸顶偏移变量。
2. **Phase 2（核心高频页）**：按 `HomeView.vue` → `SearchView.vue` → `CategoryDetailView.vue` → `TopicDetailView.vue` → `QuestionDetailView.vue` 顺序清理重复样式并按第 4 节方案落地。
3. **Phase 3（次要页与收口）**：更新 `ApiKeyView.vue`、`NotFoundView.vue`、`ListPagination.vue`，统一空态/加载态面板内边距。

### 验收清单
- [ ] `frontend` 下执行 `npm run build`（`vue-tsc -b && vite build`）通过，无类型或样式编译错误。
- [ ] 在以下 4 个典型视口宽度下逐页抽查无横向滚动条、无元素重叠或截断：`1440px`、`1024px`、`768px`、`390px`（含 `360px` 小屏）。
- [ ] 首屏可见度提升验证：
  - 首页首屏可直接看到搜索区 + 至少首行分类卡片；
  - 分类详情页首屏可直接看到首行主题卡片；
  - 主题详情页首屏可直接看到至少 3–4 条问题卡片；
  - 搜索页在移动端滚动结果列表时，筛选栏与结果卡片不被过高的吸顶区域遮挡；
  - 问题详情页首屏可直接看到题目与答案开头。
- [ ] 各视图 `<style scoped>` 中已无同选择器多段覆盖的冗余样式。
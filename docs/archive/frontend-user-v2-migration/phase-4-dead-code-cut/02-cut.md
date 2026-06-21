# 02 · 执行删除

> 7 删 + 4 改。按子步推进，每步可独立 grep 验证。

---

## Step 2.1 · 删 6 个文件 + 1 个目录

```bash
git rm src/components/Dia.vue \
       src/stores/dia.ts \
       src/components/Navigator.vue \
       src/components/Link/LinkBoxTitle.vue \
       src/stores/routers.ts

git rm -r src/utils/aurora-dia
```

**沙盒实测输出**：

```
rm 'src/components/Dia.vue'
rm 'src/components/Link/LinkBoxTitle.vue'
rm 'src/components/Navigator.vue'
rm 'src/stores/dia.ts'
rm 'src/stores/routers.ts'
rm 'src/utils/aurora-dia/index.ts'
rm 'src/utils/aurora-dia/messages/en.json'
rm 'src/utils/aurora-dia/messages/zh-CN.json'
rm 'src/utils/aurora-dia/messages/zh-TW.json'
```

合计 9 个文件 staged 为 deleted。

> ⚠️ **本步骤不删 `src/stores/navigator.ts`**，它要保留并在 Step 2.4 改造。
> ⚠️ **本步骤不删 `src/utils/comments/`**，留 Phase 7g。

---

## Step 2.2 · 改 `src/App.vue`

**改动 1**：删模板里 Dia + Navigator 注释

```diff
   <template v-if="isMobile">
     <MobileMenu />
   </template>
-  <!-- <Navigator /> -->
-  <Dia v-if="!isMobile && configReady" />
   <teleport to="head">
```

**改动 2**：删 import

```diff
 import HeaderMain from '@/components/Header/src/Header.vue'
 import FooterContainer from '@/components/Footer/FooterContainer.vue'
-import Navigator from '@/components/Navigator.vue'
 import MobileMenu from '@/components/MobileMenu.vue'
-import Dia from '@/components/Dia.vue'
 import defaultCover from '@/assets/default-cover.jpg'
```

**改动 3**：删 components 注册

```diff
   components: {
     HeaderMain,
     FooterContainer,
-    Navigator,
     MobileMenu,
-    Dia,
     VueEasyLightbox,
     FooterLink
   },
```

---

## Step 2.3 · 改 `src/models/ThemeConfig.class.ts`

**改动 1**：删 `PluginsData` interface 里的 `aurora_bot` 块

```diff
   copy_protection: {
     enable: boolean
     author: { cn: string; en: string }
     link: { cn: string; en: string }
     license: { cn: string; en: string }
   }
-
-  aurora_bot: {
-    enable: boolean
-    locale: string
-    bot_type: string
-    tips: { [key: string]: { selector: string; text: string | string[] } }
-  }
 }
```

**改动 2**：删 `Plugins` class 里的 `aurora_bot` 字段

```diff
   copy_protection = {
     enable: true,
     author: { cn: '', en: '' },
     link: { cn: '', en: '' },
     license: { cn: '', en: '' }
   }
-  aurora_bot = {
-    enable: false,
-    locale: 'en',
-    bot_type: 'dia',
-    tips: {}
-  }

   /**
    * Model class for Site meta settings
    */
```

> 不需要改 `public/api/site.json`——沙盒确认它里面没有 `aurora_bot` 字段（默认值由 ThemeConfig class 提供，mock 没显式设置）。

---

## Step 2.4 · 改 `src/stores/navigator.ts`（**只切 Navigator 弹窗那一半**）

**完整新内容**：

```ts
import { defineStore } from 'pinia'

export const useNavigatorStore = defineStore({
  id: 'navigatorStore',
  state: () => ({
    openMenu: false,
    isDone: false,
    progress: 0
  }),
  getters: {},
  actions: {
    toggleMobileMenu() {
      this.isDone = false
      this.openMenu = !this.openMenu
      setTimeout(() => {
        this.isDone = this.openMenu
      }, 300)
    },
    updateProgress(progress: number) {
      this.progress = progress
    }
  }
})
```

**diff 视角**：

```diff
   state: () => ({
     openMenu: false,
-    openNavigator: false,
     isDone: false,
     progress: 0
   }),
   getters: {},
   actions: {
     toggleMobileMenu() { ... },
-    toggleOpenNavigator() {
-      this.openNavigator = !this.openNavigator
-    },
-    setOpenNavigator(status: boolean) {
-      this.openNavigator = status
-    },
     updateProgress(progress: number) { ... }
   }
```

---

## Step 2.5 · 改 `src/components/MobileMenu.vue`

第 174 行那一句 `setOpenNavigator(false)` 调用要删（Navigator 弹窗已不存在，关闭它没意义）：

```diff
     const pushPage = (path: string): void => {
       if (!path) return
       navigatorStore.toggleMobileMenu()
-      navigatorStore.setOpenNavigator(false)
       if (path.match(/(http:\/\/|https:\/\/)((\w|=|\?|\.|\/|&|-)+)/g)) {
         window.location.href = path
```

---

## Step 2.6 · 验证 + commit

继续到 [03-verify.md](./03-verify.md) 跑 5 项出口验收，全过后再回来 commit。

```bash
git status
# 期望：
#   deleted:   src/components/Dia.vue
#   deleted:   src/components/Link/LinkBoxTitle.vue
#   deleted:   src/components/Navigator.vue
#   deleted:   src/stores/dia.ts
#   deleted:   src/stores/routers.ts
#   deleted:   src/utils/aurora-dia/index.ts
#   deleted:   src/utils/aurora-dia/messages/en.json
#   deleted:   src/utils/aurora-dia/messages/zh-CN.json
#   deleted:   src/utils/aurora-dia/messages/zh-TW.json
#   modified:  src/App.vue
#   modified:  src/components/MobileMenu.vue
#   modified:  src/models/ThemeConfig.class.ts
#   modified:  src/stores/navigator.ts

git add src/App.vue src/components/MobileMenu.vue src/models/ThemeConfig.class.ts src/stores/navigator.ts

git commit -m "chore(phase-4): dead code cut (Dia, Navigator, LinkBoxTitle, routers store)"

git tag phase-4-done
```

> ⚠️ **commitlint 限制**：本仓库启用了 `@commitlint/config-conventional`，commit subject 必须是 `<type>(<scope>): <description>` 格式，header ≤ 100 字。`type` 只能是 `chore` / `feat` / `fix` / `refactor` / `docs` 等约定值（不能用 `phase` 当 type）。详见 [04-troubleshooting.md case B](./04-troubleshooting.md)。

---

→ [03-verify.md](./03-verify.md)

<template>
  <ul
    class="flex flex-row justify-evenly flex-wrap w-full py-4 px-2 text-center items-center"
  >
    <a
      :href="socials.github"
      target="_blank"
      ref="github"
      v-if="socials.github"
    >
      <li class="diamond-clip-path diamond-icon">
        <SvgIcon icon-class="github" class="fill-current" />
      </li>
    </a>
    <a
      :href="socials.twitter"
      target="_blank"
      ref="twitter"
      v-if="socials.twitter"
    >
      <li class="diamond-clip-path diamond-icon">
        <SvgIcon icon-class="twitter" class="fill-current" />
      </li>
    </a>
    <a
      :href="socials.stackoverflow"
      target="_blank"
      ref="stackoverflow"
      v-if="socials.stackoverflow"
    >
      <li class="diamond-clip-path diamond-icon">
        <SvgIcon icon-class="stackoverflow" class="fill-current" />
      </li>
    </a>
    <a
      :href="socials.wechat"
      target="_blank"
      ref="wechat"
      v-if="socials.wechat"
    >
      <li class="diamond-clip-path diamond-icon">
        <SvgIcon icon-class="wechat" class="fill-current" />
      </li>
    </a>
    <a :href="socials.qq" target="_blank" ref="qq" v-if="socials.qq">
      <li class="diamond-clip-path diamond-icon">
        <SvgIcon icon-class="qq" class="fill-current" />
      </li>
    </a>
    <a :href="socials.weibo" target="_blank" ref="weibo" v-if="socials.weibo">
      <li class="diamond-clip-path diamond-icon">
        <SvgIcon icon-class="weibo" class="fill-current" />
      </li>
    </a>
    <a :href="socials.csdn" target="_blank" ref="csdn" v-if="socials.csdn">
      <li class="diamond-clip-path diamond-icon">
        <SvgIcon icon-class="csdn" class="fill-current" />
      </li>
    </a>
    <a :href="socials.zhihu" target="_blank" ref="zhifu" v-if="socials.zhihu">
      <li class="diamond-clip-path diamond-icon">
        <SvgIcon icon-class="zhifu" class="fill-current" />
      </li>
    </a>
    <a
      :href="socials.juejin"
      target="_blank"
      ref="juejin"
      v-if="socials.juejin"
    >
      <li class="diamond-clip-path diamond-icon">掘</li>
    </a>
    <template v-if="customSocials.length > 0">
      <template v-for="social in customSocials" :key="social.name">
        <a :href="social.link" target="_blank" :ref="social.name">
          <li class="diamond-clip-path diamond-icon">
            <template v-if="social.icon.img_link">
              <SvgIcon
                :icon-class="social.icon.img_link"
                class="fill-current"
              />
            </template>
            <template v-else>
              <i :class="['custom-social-svg-icon', social.icon.iconfont]" />
            </template>
          </li>
        </a>
      </template>
    </template>
    <a
      :href="socials.linkedin"
      target="_blank"
      rel="noreferrer"
      ref="linkedin"
      v-if="socials.linkedin"
    >
      <li class="diamond-clip-path diamond-icon" aria-label="LinkedIn">in</li>
    </a>
    <a
      :href="socials.qiita"
      target="_blank"
      rel="noreferrer"
      ref="qiita"
      v-if="socials.qiita"
    >
      <li class="diamond-clip-path diamond-icon" aria-label="Qiita">Qiita</li>
    </a>
    <a
      :href="socials.website"
      target="_blank"
      rel="noreferrer"
      ref="website"
      v-if="socials.website"
    >
      <li class="diamond-clip-path diamond-icon" aria-label="Website">Web</li>
    </a>
    <a :href="`mailto:${socials.email}`" ref="email" v-if="socials.email">
      <li class="diamond-clip-path diamond-icon" aria-label="Email">
        <SvgIcon icon-class="email" class="fill-current" />
      </li>
    </a>
  </ul>
</template>

<script setup lang="ts">
import { computed, toRefs } from 'vue'
import SvgIcon from '@/components/SvgIcon/index.vue'

const props = defineProps({
  socials: {
    type: Object,
    default: () => {
      return {}
    }
  }
})

const socials = toRefs(props).socials
const customSocials = computed(() => socials.value.customs.socials)
</script>

<style scoped>
.custom-social-SvgIcon {
  width: 1em;
  height: 1em;
  font-size: 1em;
  vertical-align: -0.15em;
  fill: var(--text-bright);
  stroke: var(--background-primary);
  overflow: hidden;
}
</style>

<template>
  <el-drawer v-model="visible" direction="rtl" :with-header="false" :before-close="handleClose">
    <span class="text font-semibold text-2xl">{{ t('user.title') }}</span>
    <template v-if="userInfo !== ''">
      <span class="text font-medium">({{ t('user.privacy_notice') }})</span>
      <div class="max-w-full mt-10">
        <button id="pick-avatar" @click="showCropper = true">
          <el-avatar :size="110" :src="userInfo.avatar" class="ml-40" />
        </button>
        <avatar-cropper
          v-model="showCropper"
          @uploaded="handleSuccess"
          trigger="#pick-avatar"
          :request-options="options"
          upload-url="/api/users/avatar" />
        <el-form>
          <el-form-item model="userInfo" :label="t('user.nickname') + ':'" class="mt-5">
            <el-input v-model="userInfo.nickname" />
          </el-form-item>
          <el-form-item model="userInfo" :label="t('user.website') + ':'" class="mt-5">
            <el-input v-model="userInfo.website" :placeholder="t('user.website_placeholder')" />
          </el-form-item>
          <el-form-item model="userInfo" :label="t('user.description') + ':'" class="mt-5">
            <el-input v-model="userInfo.intro" :placeholder="t('user.intro_placeholder')" />
          </el-form-item>
          <el-form-item model="userInfo" :label="t('user.email') + ':'" class="mt-5">
            <el-input disabled :placeholder="userInfo.email">
              <template #append v-if="userInfo.email === null">
                <span class="text" @click="changeEmailDialogVisible">{{ t('user.bind') }}</span>
              </template>
              <template #append v-else>
                <span class="text" @click="changeEmailDialogVisible">{{ t('user.modify') }}</span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item :label="t('user.subscribe') + ':'">
            <el-switch
              v-model="userInfo.isSubscribe"
              :loading="loading"
              :before-change="beforeChange"
              @change="changeSubscribe"
              active-color="#0fb6d6"
              :active-value="1"
              :inactive-value="0" />
          </el-form-item>
          <button
            @click="commit"
            type="button"
            id="submit-button"
            class="mt-5 w-20 text-white p-2 rounded-lg transition transform hover:scale-105 flex float-right">
            <span class="text-center flex-grow commit">{{ t('user.submit') }}</span>
          </button>
        </el-form>
      </div>
    </template>
    <br />
    <br />
  </el-drawer>
  <el-dialog v-model="emailDialogVisible" width="30%">
    <el-form>
      <el-form-item model="userInfo" class="mt-5">
        <el-input v-model="email" :placeholder="t('user.email_placeholder')" />
      </el-form-item>
      <el-form-item model="userInfo" type="password" class="mt-8">
        <el-input v-model="VerificationCode" type="password" :placeholder="t('user.code_placeholder')">
          <template #append>
            <button type="button" style="outline: none">
              <span class="text" @click="sendCode">{{ message }}</span>
            </button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="bingingEmail" size="large" class="mx-auto mt-3">{{ t('user.bind') }}</el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script lang="ts">
import { defineComponent, toRef, ref, reactive, toRefs, getCurrentInstance, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import AvatarCropper from 'vue-avatar-cropper'
import api from '@/api/api'
import { useI18n } from 'vue-i18n'

export default defineComponent({
  name: 'UserCenter',
  components: { AvatarCropper },
  setup() {
    const { t } = useI18n()
    const proxy: any = getCurrentInstance()?.appContext.config.globalProperties
    const userStore = useUserStore()
    const reactiveData = reactive({
      message: t('auth.send_code'),
      emailDialogVisible: false,
      email: '' as any,
      VerificationCode: '' as any,
      loading: false,
      switchState: false
    })
    let showCropper = ref(false)
    const handleClose = () => {
      userStore.userVisible = false
    }
    const changeEmailDialogVisible = () => {
      reactiveData.emailDialogVisible = true
    }
    const bingingEmail = () => {
      let params = {
        email: reactiveData.email,
        code: reactiveData.VerificationCode
      }
      api.bindingEmail(params).then(({ data }) => {
        if (data.flag) {
          proxy.$notify({
            title: t('notify.success'),
            message: t('notify.bind_success'),
            type: 'success'
          })
          userStore.userInfo.email = reactiveData.email
          reactiveData.emailDialogVisible = false
        }
      })
    }
    const handleSuccess = (data: any) => {
      data.response.json().then((data: any) => {
        if (data.flag) {
          userStore.userInfo.avatar = data.data
          proxy.$notify({
            title: t('notify.success'),
            message: t('notify.upload_success'),
            type: 'success'
          })
        }
      })
    }
    const changeSubscribe = () => {
      if (reactiveData.switchState) {
        let params = {
          userId: userStore.userInfo.userInfoId,
          isSubscribe: userStore.userInfo.isSubscribe
        }
        api.updateUserSubscribe(params).then(({ data }) => {
          if (data.flag) {
            proxy.$notify({
              title: t('notify.success'),
              message: t('notify.update_success'),
              type: 'success'
            })
          }
        })
      }
    }
    const commit = () => {
      let params = {
        nickname: userStore.userInfo.nickname,
        website: userStore.userInfo.website,
        intro: userStore.userInfo.intro
      }
      api.submitUserInfo(params).then(({ data }) => {
        if (data.flag) {
          proxy.$notify({
            title: t('notify.success'),
            message: t('notify.update_success'),
            type: 'success'
          })
        }
      })
    }
    const sendCode = () => {
      api.sendValidationCode(reactiveData.email).then(({ data }) => {
        if (data.flag) {
          proxy.$notify({
            title: t('notify.success'),
            message: t('notify.code_sent'),
            type: 'success'
          })
        }
      })
    }
    const beforeChange = () => {
      reactiveData.switchState = true
      reactiveData.loading = true
      return new Promise((resolve, reject) => {
        if (userStore.userInfo.email === '' || userStore.userInfo.email === null) {
          reactiveData.loading = false
          proxy.$notify({
            title: t('notify.warning'),
            message: t('notify.email_not_bound'),
            type: 'warning'
          })
          return reject(new Error('Error'))
        } else {
          reactiveData.loading = false
          return resolve(true)
        }
      })
    }
    return {
      userInfo: toRef(userStore.$state, 'userInfo'),
      ...toRefs(reactiveData),
      visible: toRef(userStore.$state, 'userVisible'),
      showCropper,
      handleClose,
      bingingEmail,
      changeEmailDialogVisible,
      changeSubscribe,
      handleSuccess,
      sendCode,
      commit,
      beforeChange,
      t,
      options: computed(() => {
        return {
          method: 'POST',
          headers: {
            Authorization: 'Bearer ' + userStore.token
          }
        }
      })
    }
  }
})
</script>
<style lang="scss" scoped>
#submit-button {
  outline: none;
  background: #0fb6d6;
}
.text {
  color: var(--text-normal);
  cursor: pointer;
}
#pick-avatar {
  outline: none;
}
</style>
<style lang="scss">
.el-form-item__label {
  text-align: left;
  width: 70px;
  color: var(--text-normal) !important;
}
.el-input__inner {
  color: var(--text-normal) !important;
  background-color: var(--background-primary-alt) !important;
}
.el-input__wrapper {
  background: var(--background-primary-alt) !important;
}
.bangding-button {
  outline: none !important;
}
</style>

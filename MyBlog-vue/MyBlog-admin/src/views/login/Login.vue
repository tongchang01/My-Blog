<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-title">管理员登录</div>
      <el-form status-icon :model="loginForm" :rules="rules" ref="ruleForm" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            prefix-icon="el-icon-user-solid"
            placeholder="用户名"
            @keyup.enter.native="login" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            prefix-icon="iconfont el-icon-mymima"
            show-password
            placeholder="密码"
            @keyup.enter.native="login" />
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="login">登录</el-button>
    </div>
  </div>
</template>

<script>
import { generaMenu } from '@/assets/js/menu'
export default {
  data: function () {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
        password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
      }
    }
  },
  methods: {
    login() {
      this.$refs.ruleForm.validate((valid) => {
        if (valid) {
          const that = this
          let param = new URLSearchParams()
          param.append('username', that.loginForm.username)
          param.append('password', that.loginForm.password)
          that.axios.post('/api/users/login', param).then(({ data }) => {
            if (data.flag) {
              that.$store.commit('login', data.data)
              generaMenu()
              that.$message.success('登录成功')
              that.$router.push({ path: '/' })
            } else {
              that.$message.error(data.message)
            }
          })
        } else {
          return false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  left: 0;

  background: url(https://tyb-blog-s3.s3.ap-northeast-1.amazonaws.com/aurora/articles/010de3a4c3f8d2cc89d2468e5e5aeb41.jpg)
    center center / cover no-repeat;

  display: flex;
  justify-content: flex-start;
  align-items: flex-end;
  padding-left: 90px;
  padding-bottom: 90px;
  box-sizing: border-box;
}

.login-card {
  width: 380px;
  padding: 42px 44px 46px;

  background: rgba(25, 20, 18, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 18px;

  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);

  box-sizing: border-box;
}

.login-title {
  color: #f5efe7;
  font-weight: bold;
  font-size: 1.05rem;
  letter-spacing: 1px;
}

.login-form {
  margin-top: 1.4rem;
}

.login-card button {
  margin-top: 1rem;
  width: 100%;
  height: 42px;

  background: linear-gradient(135deg, #c58b45, #9f6428);
  border: none;
  border-radius: 8px;

  color: #fff;
  font-weight: bold;
}

.login-card button:hover {
  background: linear-gradient(135deg, #d89b52, #ad7130);
}

/* 输入框整体 */
.login-card >>> .el-input__inner {
  height: 42px;

  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 8px;

  color: #2c211b;
}

/* placeholder */
.login-card >>> .el-input__inner::placeholder {
  color: #8a7d72;
}

/* icon颜色 */
.login-card >>> .el-input__prefix,
.login-card >>> .el-input__suffix {
  color: #8a6a4a;
}
</style>

<template>
  <el-dialog
    :model-value="modelValue"
    title="修改登录密码"
    width="460px"
    align-center
    destroy-on-close
    @update:model-value="(v) => emit('update:modelValue', v)"
    @closed="reset"
  >
    <div class="tip-bar">
      当前账号：<b>{{ userStore.userInfo?.empNo }}</b> {{ userStore.userInfo?.name }}
      <span class="tip-sub">修改成功后其他已登录的会话会自动失效</span>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入当前登录密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="6-32 位，建议字母加数字" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSubmit">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const userStore = useUserStore()
const formRef = ref()
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== form.newPassword) return callback(new Error('两次输入的新密码不一致'))
  callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }]
}

function reset() {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.newPassword === form.oldPassword) {
    ElMessage.warning('新密码不能与原密码相同')
    return
  }
  saving.value = true
  try {
    await userStore.changePassword(form.oldPassword, form.newPassword)
    ElMessage.success('密码修改成功，请牢记新密码')
    emit('update:modelValue', false)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip-bar {
  font-size: 12.5px;
  color: #52627a;
  background: #f5f8ff;
  border: 1px solid #e0e9ff;
  border-radius: 8px;
  padding: 9px 12px;
  margin-bottom: 16px;
  line-height: 1.6;
}

.tip-bar b {
  color: #2f5fe0;
}

.tip-sub {
  display: block;
  color: #97a3b6;
  font-size: 12px;
  margin-top: 2px;
}
</style>

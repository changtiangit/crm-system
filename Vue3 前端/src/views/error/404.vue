<template>
  <div class="not-found">
    <div class="code">404</div>
    <h2>页面走丢了</h2>
    <p>你访问的页面不存在，或已被移除。</p>
    <el-button type="primary" @click="$router.replace('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #f4f6fa;
}
.code {
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #5b8cff, #2f5fe0);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
h2 {
  margin: 8px 0 0;
  font-size: 20px;
  color: #273449;
}
p {
  margin: 0 0 14px;
  font-size: 13.5px;
  color: #8493a8;
}
</style>

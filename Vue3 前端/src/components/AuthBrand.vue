<template>
  <section class="brand-panel">
    <div class="brand-glow brand-glow-1"></div>
    <div class="brand-glow brand-glow-2"></div>

    <header class="brand-header">
      <div class="brand-logo">CRM</div>
      <span class="brand-name">客户关系管理系统</span>
    </header>

    <div class="brand-main">
      <h1>让每一次连接<br />都成为增长的起点</h1>
      <p class="brand-sub">线索 · 客户 · 商机 · 订单 · 回款 · 售后，全流程一体化管理</p>

      <ul class="brand-points">
        <li>
          <span class="dot"></span>
          <div>
            <strong>线索集中管理</strong>
            <em>多渠道线索统一沉淀，自动分配跟进</em>
          </div>
        </li>
        <li>
          <span class="dot"></span>
          <div>
            <strong>商机推进可视</strong>
            <em>阶段、金额、预计成交日一目了然</em>
          </div>
        </li>
        <li>
          <span class="dot"></span>
          <div>
            <strong>经营数据一屏掌握</strong>
            <em>销售、客户采购、商品毛利三张报表可钻取到单</em>
          </div>
        </li>
        <li>
          <span class="dot"></span>
          <div>
            <strong>权限分级可控</strong>
            <em>员工、角色、部门灵活配置</em>
          </div>
        </li>
      </ul>
    </div>

    <footer class="brand-footer">© 2026 CRM System · 企业级客户管理平台</footer>
  </section>
</template>

<style scoped>
.brand-panel {
  position: relative;
  width: 52%;
  max-width: 720px;
  padding: 44px 56px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(150deg, #1b2a52 0%, #16305f 42%, #1a4bb5 100%);
}

.brand-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  pointer-events: none;
}
.brand-glow-1 {
  width: 460px;
  height: 460px;
  right: -140px;
  top: -120px;
  background: radial-gradient(circle, rgba(88, 140, 255, 0.55), transparent 68%);
}
.brand-glow-2 {
  width: 520px;
  height: 520px;
  left: -180px;
  bottom: -220px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.32), transparent 68%);
}

.brand-header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 0.5px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(6px);
}

.brand-name {
  font-size: 16px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.9);
}

.brand-main {
  position: relative;
  max-width: 460px;
}

.brand-main h1 {
  font-size: 38px;
  line-height: 1.35;
  margin: 0 0 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.brand-sub {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.72);
  margin: 0 0 44px;
}

.brand-points {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.brand-points li {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.brand-points .dot {
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: #7fb0ff;
  box-shadow: 0 0 0 4px rgba(127, 176, 255, 0.18);
  flex-shrink: 0;
}

.brand-points strong {
  display: block;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.brand-points em {
  font-style: normal;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.brand-footer {
  position: relative;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

@media (max-width: 960px) {
  .brand-panel {
    display: none;
  }
}
</style>

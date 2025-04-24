function initAddressStatus(layui) {
    const form = layui.form;
    const layer = layui.layer;

    // 格式化数字，保留最多6位小数，去除末尾零
    function formatNumber(num) {
        if (num === null || num === undefined) return '0';
        if (Number.isInteger(num)) return num.toString();
        return parseFloat(num.toFixed(6)).toString().replace(/\.?0+$/, '');
    }

    // 格式化时间，直接返回字符串
    function formatDate(timeStr) {
        return timeStr || '-';
    }

    // 查询模式切换
    form.on('select(queryMode)', function (data) {
        const singleInput = document.querySelector('.single-input');
        const batchInput = document.querySelector('.batch-input');
        if (data.value === 'single') {
            singleInput.style.display = 'block';
            batchInput.style.display = 'none';
        } else {
            singleInput.style.display = 'none';
            batchInput.style.display = 'block';
        }
    });

    // 查询按钮事件
    document.getElementById('queryBtn').addEventListener('click', async () => {
        const queryBtn = document.getElementById('queryBtn');
        const mode = document.getElementById('queryMode').value;
        let addresses = [];

        // 获取输入地址
        if (mode === 'single') {
            const address = document.getElementById('singleAddress').value.trim();
            if (!address) {
                layer.msg('请输入 TRC20 地址', { icon: 2 });
                return;
            }
            if (!address.startsWith('T') || address.length !== 34) {
                layer.msg('请输入有效的 TRC20 地址', { icon: 2 });
                return;
            }
            addresses = [address];
        } else {
            const batchInput = document.getElementById('batchAddresses').value.trim();
            if (!batchInput) {
                layer.msg('请输入至少一个 TRC20 地址', { icon: 2 });
                return;
            }
            addresses = batchInput.split('\n').map(addr => addr.trim()).filter(addr => addr);
            if (addresses.length === 0) {
                layer.msg('请输入有效的 TRC20 地址', { icon: 2 });
                return;
            }
            for (const addr of addresses) {
                if (!addr.startsWith('T') || addr.length !== 34) {
                    layer.msg(`地址 ${addr} 无效`, { icon: 2 });
                    return;
                }
            }
        }

        // 禁用按钮
        queryBtn.classList.add('layui-btn-disabled');
        queryBtn.disabled = true;
        const loading = layer.load(2);

        try {
            // 调用 API
            const response = await fetch('/wallets/getaddressstatus', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ addresses })
            });

            if (response.ok) {
                const result = await response.json();
                if (result.code === 200 && result.data) {
                    const resultBody = document.getElementById('resultBody');
                    const resultTable = document.getElementById('resultTable');
                    resultBody.innerHTML = ''; // 清空表格

                    // 填充表格
                    result.data.forEach(item => {
                        const remainingBandwidth = item.bandwidth ? formatNumber(item.bandwidth.total - item.bandwidth.used) : '0';
                        const row = document.createElement('tr');
                        row.innerHTML = `
                            <td>${item.address}</td>
                            <td><span class="status ${item.isActive ? 'active' : 'inactive'}">${item.isActive ? '已激活' : '未激活'}</span></td>
                            <td><img src="https://static.tronscan.org/production/logo/trx.png" alt="TRX" style="height: 16px; vertical-align: middle;"> ${formatNumber(item.trxBalance)} TRX</td>
                            <td><img src="https://static.tronscan.org/production/logo/usdtlogo.png" alt="USDT" style="height: 16px; vertical-align: middle;"> ${formatNumber(item.usdtBalance)} USDT</td>
                            <td>${formatDate(item.createTime)}</td>
                            <td>${formatDate(item.lastUsdtSendTime)}</td>
                            <td>${formatDate(item.lastUsdtReceiveTime)}</td>
                            <td>${remainingBandwidth}</td>
                            <td>${formatNumber(item.energy)}</td>
                        `;
                        resultBody.appendChild(row);
                    });

                    // 显示表格
                    resultTable.style.display = 'table';
                } else {
                    layer.msg(result.msg || '查询失败，请稍后重试', { icon: 2 });
                }
            } else {
                layer.msg(`服务器错误: ${response.status}`, { icon: 2 });
            }
        } catch (error) {
            console.error('API Error:', error);
            layer.msg('网络错误，请检查连接', { icon: 2 });
        } finally {
            layer.close(loading);
            queryBtn.classList.remove('layui-btn-disabled');
            queryBtn.disabled = false;
        }
    });

    // 初始化表单
    form.render();
}
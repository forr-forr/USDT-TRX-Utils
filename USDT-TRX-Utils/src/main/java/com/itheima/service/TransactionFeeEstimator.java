package com.itheima.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TransactionFeeEstimator 接口
 * 用于估算 TRX 和 USDT 转账的手续费，包括正式环境与测试环境的接口方法。
 */
public interface TransactionFeeEstimator {


    /**
     * 正式环境 - 计算 TRX 转账手续费
     * <p>
     * 该方法用于在正式环境中估算 TRX 转账的实际手续费。
     * 会考虑带宽消耗，并返回详细的费用信息。
     *
     * @param sender    发送方地址 (Base58 格式)
     * @param recipient 接收方地址 (Base58 格式)
     * @param amount    转账的 TRX 金额
     * @return JSON 格式的字符串，包含总 TRX 费用和带宽消耗信息
     */
    String TRX_TransactionFee(String sender, String recipient, double amount);

    /**
     * 正式环境 - 计算 USDT (TRC20) 转账手续费
     * <p>
     * 该方法用于在正式环境中估算 USDT 转账的实际手续费。
     * 会考虑能量消耗、带宽消耗，并返回详细的费用信息。
     *
     * @param sender    发送方地址 (Base58 格式)
     * @param recipient 接收方地址 (Base58 格式)
     * @param amount    转账的 USDT 金额
     * @return JSON 格式的字符串，包含总 TRX 费用、能量消耗和带宽消耗信息
     */
    String USDT_TransactionFee(String sender, String recipient, double amount);

    /**
     *  获取地址信息
     * @param sender
     * @return
     */
    String getAddressInfo(String sender);




}

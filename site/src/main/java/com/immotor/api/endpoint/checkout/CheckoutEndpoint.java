/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.immotor.api.endpoint.checkout;

import com.alipay.util.httpClient.AlipayNotify;
import com.immotor.sample.payment.service.gateway.NullPaymentGatewayWebResponseServiceImpl;
import com.wechat.constant.GlobalConfig;
import com.wechat.util.GetWxOrderno;
import com.wechat.util.RequestHandler;
import com.wechat.util.TenpayUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.payment.PaymentGatewayType;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponsePrintService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.common.web.payment.controller.PaymentGatewayAbstractController;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.web.api.wrapper.OrderPaymentWrapper;
import org.broadleafcommerce.core.web.api.wrapper.OrderWrapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * This is a reference REST API endpoint for checkout. This can be modified, used as is, or removed.
 * The purpose is to provide an out of the box RESTful checkout service implementation, but also
 * to allow the implementor to have fine control over the actual API, URIs, and general JAX-RS annotations.
 *
 * @author Kelly Tisdell
 */
@RestController
@RequestMapping(value = "/cart/checkout/",
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class CheckoutEndpoint extends org.broadleafcommerce.core.web.api.endpoint.checkout.CheckoutEndpoint {
    private static final Log LOG = LogFactory.getLog(CheckoutEndpoint.class.getName());
    @Resource(name = "blNullPaymentGatewayController")
    protected PaymentGatewayAbstractController paymentGatewayAbstractController;

    @Resource(
            name = "blPaymentGatewayWebResponsePrintService"
    )
    protected PaymentGatewayWebResponsePrintService webResponsePrintService;
    @Override
    @RequestMapping(value = "payments", method = RequestMethod.GET)
    public List<OrderPaymentWrapper> findPaymentsForOrder(HttpServletRequest request) {
        return super.findPaymentsForOrder(request);
    }

    @Override
    @RequestMapping(value = "payment", method = RequestMethod.POST)
    public OrderPaymentWrapper addPaymentToOrder(HttpServletRequest request,
                                                 @RequestBody OrderPaymentWrapper wrapper) {
        return super.addPaymentToOrder(request, wrapper);
    }

    @Override
    @RequestMapping(value = "payment", method = RequestMethod.DELETE)
    public OrderWrapper removePaymentFromOrder(HttpServletRequest request,
                                               OrderPaymentWrapper wrapper) {
        return super.removePaymentFromOrder(request, wrapper);
    }

    @Override
    @RequestMapping(method = RequestMethod.POST)
    public OrderWrapper performCheckout(HttpServletRequest request) {
        return super.performCheckout(request);
    }

    @RequestMapping(value = "alipay_return", method = RequestMethod.GET)
    public String returnUrl(HttpServletRequest request) {
        String result = validateAliPay(request);
        try {
            if ("C".equals(result)) {
                result = paymentGatewayAbstractController.process(null, request, null);
                if (result.startsWith(paymentGatewayAbstractController.getBaseConfirmationRedirect())) {
                    int start = result.lastIndexOf("/");
                    return result.substring(start + 1);
                }
            } else if (!"false".equals(result)) {
                return result;
            }

        } catch (PaymentException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "pay_wechat/query", method = RequestMethod.GET)
    public String queryPaymentResult(HttpServletRequest request, HttpServletResponse response, @RequestParam String orderId) throws Exception {

        Order order = orderService.findOrderById(Long.parseLong(orderId));
        if (null == order) {
            return "error orderId";
        } else if (null != order.getOrderNumber()) {
            return order.getOrderNumber();
        }
        String currTime = TenpayUtil.getCurrTime();
        String strTime = currTime.substring(8, currTime.length());
        String strRandom = TenpayUtil.buildRandom(4) + "";
        String nonce_str = strTime + strRandom;

        SortedMap<String, String> packageParams = new TreeMap<String, String>();
        packageParams.put("appid", GlobalConfig.APPID);
        packageParams.put("mch_id", GlobalConfig.MCH_ID);
        packageParams.put("nonce_str", nonce_str);

        packageParams.put("out_trade_no", orderId);

        RequestHandler requestHandler = new RequestHandler(request, response);
        requestHandler.init(GlobalConfig.APPID, GlobalConfig.APPSECRET, GlobalConfig.KEY);
        String sign = requestHandler.createSign(packageParams);
        String xml = "<xml>" +
                "<appid>" + GlobalConfig.APPID + "</appid>" +
                "<mch_id>" + GlobalConfig.MCH_ID + "</mch_id>" +
                "<nonce_str>" + nonce_str + "</nonce_str>" +
                "<out_trade_no>" + orderId + "</out_trade_no>" +
                "<sign>" + sign + "</sign>" +
                "</xml>";
        boolean queryResult = GetWxOrderno.queryOrder(GlobalConfig.query_url, xml);
        if (queryResult) {
            PaymentResponseDTO e = new PaymentResponseDTO(NullPaymentGatewayWebResponseServiceImpl.WECHAT_PAYMENT,
                    PaymentGatewayType.PASSTHROUGH).paymentTransactionType(PaymentTransactionType.AUTHORIZE_AND_CAPTURE)
                    .rawResponse(webResponsePrintService.printRequest(request));
            e.successful(true).valid(true).orderId(orderId)
                    .amount(order.getTotal())
                    .rawResponse(request.getRequestURI());
            paymentGatewayAbstractController.applyPaymentToOrder(e);
            if (!e.isSuccessful()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("The Response DTO is marked as unsuccessful. Delegating to the payment module to handle an unsuccessful transaction");
                }

                return null;
            } else if (!e.isValid()) {
                throw new PaymentException("The validity of the response cannot be confirmed.Check the Tamper Proof Seal for more details.");
            } else {
//                String orderId = e.getOrderId();
                if (orderId == null) {
                    throw new RuntimeException("Order ID must be set on the Payment Response DTO");
                } else if (e.isCompleteCheckoutOnCallback()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("The Response DTO for this Gateway is configured to complete checkout on callback. Initiating Checkout with Order ID: " + orderId);
                    }

                    String orderNumber = paymentGatewayAbstractController.initiateCheckout(Long.valueOf(orderId));
                    return orderNumber;
                } else {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("The Gateway is configured to not complete checkout. Redirecting to the Order Review Page for Order ID: " + orderId);
                    }

                    return null;
                }
            }

        } else {
            return null;
        }

    }
    /**
     * @param request
     * @return orderNumber: already success;  C: continue; {other}:failed
     */
    protected String validateAliPay(HttpServletRequest request) {
        Map<String, String> params = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        String trade_status = new String(request.getParameter("trade_status"));
        String orderId = request.getParameter("out_trade_no");

        if (AlipayNotify.verify(params)) {
            if (trade_status.equals("TRADE_FINISHED") || trade_status.equals("TRADE_SUCCESS")) {
                Order order = orderService.findOrderById(Long.parseLong(orderId));
                if (order != null || !order.getStatus().equals(OrderStatus.IN_PROCESS)) {
                    return "C";
                }
                return order.getOrderNumber();
            }
            return "false";
        } else {
            return "false";
        }
    }
}

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
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.common.web.payment.controller.PaymentGatewayAbstractController;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.web.api.wrapper.OrderPaymentWrapper;
import org.broadleafcommerce.core.web.api.wrapper.OrderWrapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    @Resource(name = "blNullPaymentGatewayController")
    protected PaymentGatewayAbstractController paymentGatewayAbstractController;

    @Override
    @RequestMapping(value = "payments", method = RequestMethod.GET)
    public List<OrderPaymentWrapper> findPaymentsForOrder(HttpServletRequest request) {
        return super.findPaymentsForOrder(request);
    }

    @Override
    @RequestMapping(value = "payment", method = RequestMethod.POST)
    public OrderPaymentWrapper addPaymentToOrder(HttpServletRequest request,
                                                 OrderPaymentWrapper wrapper) {
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

    @RequestMapping(value = "alipay_return", method = RequestMethod.POST)
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

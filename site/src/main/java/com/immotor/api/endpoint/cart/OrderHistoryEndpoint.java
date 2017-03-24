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

package com.immotor.api.endpoint.cart;

import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.web.api.wrapper.OrderWrapper;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * This is a reference REST API endpoint for order history. This can be modified, used as is, or removed.
 * The purpose is to provide an out of the box RESTful order history service implementation, but also
 * to allow the implementor to have fine control over the actual API, URIs, and general JAX-RS annotations.
 *
 * @author Kelly Tisdell
 */
@RestController
@RequestMapping(value = "/orders/",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OrderHistoryEndpoint extends org.broadleafcommerce.core.web.api.endpoint.order.OrderHistoryEndpoint {

    @Resource(name = "blCustomerService")
    private CustomerService customerService;

    @Override
    @RequestMapping(method = RequestMethod.GET)
    public List<OrderWrapper> findOrdersForCustomer(HttpServletRequest request,
                                                    @RequestParam(value = "orderStatus", defaultValue = "SUBMITTED") String orderStatus) {
        return super.findOrdersForCustomer(request, orderStatus);
    }

    @RequestMapping(value = "/{orderNumber}", method = RequestMethod.GET)
    public OrderWrapper getOrderDetail(HttpServletRequest request, @RequestParam String customerId, @PathVariable("orderNumber") String orderNumber) {
        Order order = orderService.findOrderByOrderNumber(orderNumber);
        if (null != order) {
            Customer customer = customerService.readCustomerById(Long.parseLong(customerId));
            if (null != customer && customer.equals(order.getCustomer())) {
                OrderWrapper wrapper = (OrderWrapper) this.context.getBean(OrderWrapper.class.getName());
                wrapper.wrapSummary(order, request);
                return wrapper;
            }
        }
        return null;
    }
}

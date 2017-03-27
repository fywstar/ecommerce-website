package com.immotor.api.endpoint.checkout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.core.web.api.endpoint.BaseEndpoint;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.service.AddressService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by billyang on 2017/3/27.
 */
@RestController
@RequestMapping(value = "/cart/shipping/",
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class ShippingInforEndpoint extends BaseEndpoint {
    private static final Log LOG = LogFactory.getLog(ShippingInforEndpoint.class);

    @Resource(name = "blAddressService")
    protected AddressService addressService;

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Resource(name = "blFulfillmentGroupService")
    protected FulfillmentGroupService fulfillmentGroupService;

    @RequestMapping(value = "{orderId}", method = RequestMethod.POST)
    public boolean saveShippingInfor(HttpServletRequest request, @PathVariable(value = "orderId") Long orderId, @RequestParam(value = "addressId") Long addressId, @RequestParam(value = "deliveryMessage", defaultValue = "") String deliveryMessage) {

        Order order = orderService.findOrderById(orderId);
        Address address = addressService.readAddressById(addressId);
        if (null == address || null == order) {
            return false;
        }
        FulfillmentGroup shippableFulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(order);
        if (null != shippableFulfillmentGroup) {
            shippableFulfillmentGroup.setAddress(address);
            shippableFulfillmentGroup.setDeliveryInstruction(deliveryMessage);
            try {
                orderService.save(order, true);
                return true;
            } catch (PricingException e) {
                LOG.error(e);
            }
        }

        return false;
    }
}

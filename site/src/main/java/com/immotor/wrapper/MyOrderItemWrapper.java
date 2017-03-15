package com.immotor.wrapper;

import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.web.api.wrapper.OrderItemWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * Created by billyang on 2017/3/15.
 */
@XmlRootElement(
        name = "orderItem"
)
@XmlAccessorType(XmlAccessType.FIELD)
public class MyOrderItemWrapper extends OrderItemWrapper {
    @XmlElement
    protected Map<String, Media> skuMedia;

    @Override
    public void wrapSummary(OrderItem model, HttpServletRequest request) {
        super.wrapSummary(model, request);
        if (model instanceof DiscreteOrderItem)
            this.skuMedia = ((DiscreteOrderItem) model).getSku().getSkuMedia();
        else if (model instanceof BundleOrderItem)
            this.skuMedia = ((BundleOrderItem) model).getSku().getSkuMedia();
    }
}

package com.immotor.wrapper;

import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.web.api.wrapper.OrderWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * Created by billyang on 2017/3/15.
 */
@XmlRootElement(
        name = "order"
)
@XmlAccessorType(XmlAccessType.FIELD)
public class MyOrderWrapper extends OrderWrapper {

    @XmlElement
    protected Date submitDate;

    @Override
    public void wrapSummary(Order model, HttpServletRequest request) {
        super.wrapSummary(model, request);
        this.submitDate = model.getSubmitDate();
    }


}

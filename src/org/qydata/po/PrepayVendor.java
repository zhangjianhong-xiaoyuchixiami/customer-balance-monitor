package org.qydata.po;

import java.io.Serializable;

/**
 * Created by jonhn on 2017/6/20.
 */
public class PrepayVendor implements Serializable {

    private Integer id;
    private Integer companyId;
    private Integer isPrepay;
    private Integer isSendOpposite;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public Integer getIsPrepay() {
        return isPrepay;
    }

    public void setIsPrepay(Integer isPrepay) {
        this.isPrepay = isPrepay;
    }

    public Integer getIsSendOpposite() {
        return isSendOpposite;
    }

    public void setIsSendOpposite(Integer isSendOpposite) {
        this.isSendOpposite = isSendOpposite;
    }
}

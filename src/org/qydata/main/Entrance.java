package org.qydata.main;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.qydata.po.PrepayVendor;
import org.qydata.tools.SendEmail;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by jonhn on 2017/4/19.
 */
public class Entrance {

    private static String [] to  = {"ld@qianyandata.com","it@qianyandata.com","accounting@qianyandata.com"};
    //private static String [] to  = {"zhangjianhong@qianyandata.com"};

    private static String resource = "mybatis.xml";
    private static InputStream is = Entrance.class.getClassLoader().getResourceAsStream(resource);
    private static SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(is);
    private static SqlSession session = sessionFactory.openSession();

    public static void main(String[] args) {
        List<PrepayVendor> prepayVendorList = queryPrepayVendor();
        if (prepayVendorList == null){
            return;
        }
        for (int i = 0; i < prepayVendorList.size() ; i++) {
            PrepayVendor prepayVendor = prepayVendorList.get(i);
            if (prepayVendor == null || prepayVendor.getVendorId() == null || prepayVendor.getVendorName() == null){
                continue;
            }
            List<PrepayVendor> prepayVendor_apiList = queryApiIdByVendorId(prepayVendor.getVendorId());
            if (prepayVendor_apiList == null){
                continue;
            }
            Integer balance = prepayVendor.getBalance();
            Double sumCost = 0.0;
            Double avgeCost = 0.0;
            for (int j = 0; j < prepayVendor_apiList.size(); j++) {
                PrepayVendor prepayVendor_api = prepayVendor_apiList.get(j);
                if (prepayVendor_api.getApiId() == null || prepayVendor_api.getCost() == null){
                    continue;
                }
                Integer cost = prepayVendor_api.getCost();
                Integer count =  queryCostAmount(prepayVendor_api.getApiId());
                if (count != null){
                    sumCost = sumCost + (cost/100.0)*count;
                }
                Double avgeCost_1 = queryAvgeConsume(prepayVendor_api.getApiId());
                if (avgeCost_1 != null){
                    avgeCost = avgeCost + avgeCost_1;
                }
            }
            Double result = (balance/100.0) - sumCost;
            if (result <= 3000 && result > 2500){
                isSend(prepayVendor.getVendorId(),3000,prepayVendor.getVendorName(),result,avgeCost);
                updateOtherFlag(prepayVendor.getVendorId(),3000);
                continue;
            }
            if (result <= 2500 && result > 2000 ){
                isSend(prepayVendor.getVendorId(),2500,prepayVendor.getVendorName(),result,avgeCost);
                updateOtherFlag(prepayVendor.getVendorId(),2500);
                continue;
            }
            if (result <= 2000 && result > 1500 ){
                isSend(prepayVendor.getVendorId(),2000,prepayVendor.getVendorName(),result,avgeCost);
                updateOtherFlag(prepayVendor.getVendorId(),2000);
                continue;
            }
            if (result <= 1500 && result > 1000 ){
                isSend(prepayVendor.getVendorId(),1500,prepayVendor.getVendorName(),result,avgeCost);
                updateOtherFlag(prepayVendor.getVendorId(),1500);
                continue;
            }
            if (result <= 1000 && result > 500 ){
                isSend(prepayVendor.getVendorId(),1000,prepayVendor.getVendorName(),result,avgeCost);
                updateOtherFlag(prepayVendor.getVendorId(),1000);
                continue;
            }
            if (result <= 500){
                send(prepayVendor.getVendorName(),result,avgeCost);
            }
        }
        session.close();
    }

    /**
     * 判断是否发送邮件
     * @param vid
     * @param level
     * @param name
     * @param result
     * @param avgeCost
     */
    public static void isSend(Integer vid, Integer level, String name,Double result,Double avgeCost){
        Integer flag = queryFlag(vid,level);
        if (flag == null){
            insertFlag(vid,level,-1);
            send(name,result,avgeCost);
        }else {
            if (flag == 1){
                send(name,result,avgeCost);
                updateFlag(vid,level,-1);
            }
        }
    }

    /**
     * 查询需要送发送余额不足报警的客户
     * @return
     */
    public static List<PrepayVendor> queryCompanyExt(){
        String queryCompanyExt = "org.qydata.mapper.ApiBanMapper.queryCompanyExt";
        return session.selectList(queryCompanyExt);
    }

    /**
     * 根据供应商Id查询对应产品Id
     * @param vid
     * @return
     */
    public static List<PrepayVendor> queryApiIdByVendorId(Integer vid){
        String queryApiIdByVendorId = "org.qydata.mapper.ApiBanMapper.queryApiIdByVendorId";
        List<PrepayVendor> prepayVendor_apiList =  session.selectList(queryApiIdByVendorId,vid);
        return prepayVendor_apiList;
    }

    /**
     * 查询产品扣费条数
     * @param aid
     * @return
     */
    public static Integer queryCostAmount(Integer aid){
        String queryCostAmount = "org.qydata.mapper.ApiBanMapper.queryCostAmount";
        Integer count =  session.selectOne(queryCostAmount,aid);
        return count;
    }

    /**
     * 查询产品日平均消费金额
     * @param aid
     * @return
     */
    public static Double queryAvgeConsume(Integer aid){
        String queryAvgeConsume = "org.qydata.mapper.ApiBanMapper.queryAvgeConsume";
        Double avgeCost_1 = session.selectOne(queryAvgeConsume,aid);
        return avgeCost_1;
    }

    /**
     * 发送邮件
     * @param name
     * @param result
     * @param avgeCost
     * @return
     */
    public static boolean send(String name,Double result,Double avgeCost){
        String title = name + "余额不足提醒";
        String content = "<html>" +
                "<body>" +
                "<div>" +
                "<span>"+ name + "剩余余额为：" + Math.round(result*100)/100.0 +"元，</span>" +
                "<span>"+ "按目前业务量大约推算" + name + "</span>" +
                "<span>"+ "一天平均消费：" + Math.round(avgeCost*100)/100.0 +"元，</span>" +
                "<span>"+ "剩余金额预计能使用：" + Math.round((result/avgeCost)*100)/100.0 +"天</span>" +
                "</div>" +
                "</body>" +
                "</html>";
        try {
            SendEmail.sendMail(to,title,content);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 修改发送标志
     * @param vid
     * @param level
     * @param flag
     */
    public static void updateFlag(Integer vid,Integer level,Integer flag){

        String updateSendFlag = "org.qydata.mapper.ApiBanMapper.updateSendFlag";
        Map<String,Object> updateMap = new HashMap<>();
        updateMap.put("vid",vid);
        updateMap.put("bal",level);
        updateMap.put("flag",flag);
        session.update(updateSendFlag,updateMap);
        session.commit();

    }

    /**
     * 插入发送标志
     * @param vid
     * @param level
     * @param flag
     */
    public static void insertFlag(Integer vid,Integer level,Integer flag){

        String insertSendFlag = "org.qydata.mapper.ApiBanMapper.insertSendFlag";
        Map<String,Object> insertMap = new HashMap<>();
        insertMap.put("vid",vid);
        insertMap.put("bal",level);
        insertMap.put("flag",flag);
        session.insert(insertSendFlag,insertMap);
        session.commit();

    }

    /**
     * 查询发送标志
     * @param vid
     * @param level
     * @return
     */
    public static Integer queryFlag(Integer vid,Integer level){

        String querySendFlag = "org.qydata.mapper.ApiBanMapper.querySendFlag";
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("vid",vid);
        queryMap.put("bal",level);
        return session.selectOne(querySendFlag,queryMap);
    }

    /**
     * 修改其他阶段发送标志
     * @param vid
     * @param level
     */
    public static void updateOtherFlag(Integer vid,Integer level){
        HashSet<Integer> set = new HashSet<>();
        set.add(3000);
        set.add(2500);
        set.add(2000);
        set.add(1500);
        set.add(1000);
        if (set.contains(level)){
            set.remove(level);
        }
        for (Integer lev : set) {
            updateFlag(vid,lev,1);
        }
    }

}

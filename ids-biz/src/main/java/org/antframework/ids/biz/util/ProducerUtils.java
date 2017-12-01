/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2017-11-27 00:34 创建
 */
package org.antframework.ids.biz.util;

import org.antframework.common.util.facade.FacadeUtils;
import org.antframework.ids.dal.entity.Ider;
import org.antframework.ids.dal.entity.Producer;
import org.antframework.ids.facade.info.IdsInfo;
import org.antframework.ids.facade.util.PeriodUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * id生产者工具类
 */
public class ProducerUtils {

    /**
     * 生产id
     *
     * @param ider     id提供者
     * @param producer 生产者
     * @param amount   需生产的id个数
     * @return 生产出的id
     */
    public static List<IdsInfo> produce(Ider ider, Producer producer, int amount) {
        return grow(ider, producer, amount * ider.getFactor());
    }

    /**
     * 增加
     *
     * @param ider     id提供者
     * @param producer 生产者
     * @param length   增加长度
     * @return 增加过程中产生的id
     */
    public static List<IdsInfo> grow(Ider ider, Producer producer, int length) {
        List<IdsInfo> idsInfos = new ArrayList<>();

        long newCurrentId = producer.getCurrentId() + length;
        long anchorId = producer.getCurrentId();
        while (anchorId < newCurrentId) {
            int periodIdAmount = calcPeriodIdAmount(ider, newCurrentId, anchorId);
            idsInfos.add(buildIdsInfo(ider, producer, anchorId, periodIdAmount));
            anchorId += periodIdAmount * ider.getFactor();
        }
        updateProducer(ider, producer, newCurrentId);

        return idsInfos;
    }

    // 计算anchorId所在首期内产生的id数量
    private static int calcPeriodIdAmount(Ider ider, long newCurrentId, long anchorId) {
        long anchorEndId = newCurrentId;
        if (ider.getMaxId() != null) {
            long anchorMaxId = anchorId / ider.getMaxId() * ider.getMaxId() + ider.getMaxId();
            anchorEndId = Math.min(anchorMaxId, newCurrentId);
        }
        return FacadeUtils.calcTotalPage(anchorEndId - anchorId, ider.getFactor());
    }

    // 构建批量id信息
    private static IdsInfo buildIdsInfo(Ider ider, Producer producer, long anchorId, int periodIdAmount) {
        IdsInfo info = new IdsInfo();
        BeanUtils.copyProperties(ider, info);
        if (ider.getMaxId() == null) {
            info.setPeriod(producer.getCurrentPeriod());
            info.setStartId(anchorId);
        } else {
            info.setPeriod(PeriodUtils.grow(ider.getPeriodType(), producer.getCurrentPeriod(), (int) (anchorId / ider.getMaxId())));
            info.setStartId(anchorId % ider.getMaxId());
        }
        info.setAmount(periodIdAmount);

        return info;
    }

    // 更新生产者
    private static void updateProducer(Ider ider, Producer producer, long newCurrentId) {
        if (ider.getMaxId() == null) {
            producer.setCurrentId(newCurrentId);
        } else {
            producer.setCurrentPeriod(PeriodUtils.grow(ider.getPeriodType(), producer.getCurrentPeriod(), (int) (newCurrentId / ider.getMaxId())));
            producer.setCurrentId(newCurrentId % ider.getMaxId());
        }
    }

    /**
     * id生产者比较器
     */
    public static class ProducerComparator implements Comparator<Producer> {
        /**
         * 实例
         */
        public static final ProducerComparator INSTANCE = new ProducerComparator();

        private ProducerComparator() {
        }

        @Override
        public int compare(Producer o1, Producer o2) {
            if (!StringUtils.equals(o1.getIdCode(), o2.getIdCode())) {
                throw new IllegalArgumentException(String.format("待比较的id生产者%s和%s不属于同一个id提供者，不能进行比较", o1.toString(), o2.toString()));
            }
            if (o1.getCurrentPeriod() != o2.getCurrentPeriod()) {
                if (o1.getCurrentPeriod().getTime() < o2.getCurrentPeriod().getTime()) {
                    return -1;
                } else if (o1.getCurrentPeriod().getTime() > o2.getCurrentPeriod().getTime()) {
                    return 1;
                }
            }
            if (o1.getCurrentId() < o2.getCurrentId()) {
                return -1;
            } else if (o1.getCurrentId() > o2.getCurrentId()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}

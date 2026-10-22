package com.designer.admin.framework.job;

import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.Date;

/**
 * Cron 表达式工具
 *
 * @author 雨夜终端
 */
public final class CronUtils {

    private CronUtils() {
    }

    public static boolean isValid(String cron) {
        return CronExpression.isValidExpression(cron);
    }

    public static Date nextTriggerTime(String cron) {
        try {
            CronExpression expression = new CronExpression(cron);
            return expression.getNextValidTimeAfter(new Date());
        } catch (ParseException e) {
            return null;
        }
    }
}

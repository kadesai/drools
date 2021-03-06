package org.drools.core.time.impl;

import java.util.Date;
import java.util.concurrent.Callable;

import org.drools.core.time.InternalSchedulerService;
import org.drools.core.time.Job;
import org.drools.core.time.JobContext;
import org.drools.core.time.JobHandle;
import org.drools.core.time.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultTimerJobInstance
    implements
    Callable<Void>,
    Comparable<DefaultTimerJobInstance>,
    TimerJobInstance {

    protected static transient Logger logger = LoggerFactory.getLogger(DefaultTimerJobInstance.class);
    
    private final Job                         job;
    private final Trigger                     trigger;
    private final JobContext                  ctx;
    protected final InternalSchedulerService  scheduler;
    private final JobHandle                   handle;

    public DefaultTimerJobInstance(Job job,
                          JobContext ctx,
                          Trigger trigger,
                          JobHandle handle,
                          InternalSchedulerService scheduler) {
        this.job = job;
        this.ctx = ctx;
        this.trigger = trigger;
        this.handle = handle;
        this.scheduler = scheduler;
    }
    

    public int compareTo(DefaultTimerJobInstance o) {
        return this.trigger.hasNextFireTime().compareTo( o.getTrigger().hasNextFireTime() );
    }    

    public Void call() throws Exception {
        try { 
            this.trigger.nextFireTime(); // need to pop
            if ( handle.isCancel() ) {
                return null;
            }
            this.job.execute( this.ctx );
            if ( handle.isCancel() ) {
                return null;
            }

            // our triggers allow for flexible rescheduling
            Date date = this.trigger.hasNextFireTime();
            if ( date != null ) {
                scheduler.internalSchedule( this );
            }
        }
        catch(Exception e) {
            logger.warn("Unable to execute timer job!", e);
            throw e;
        }

        return null;
    }

    public JobHandle getJobHandle() {
        return handle;
    }

    public Job getJob() {
        return job;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public JobContext getJobContext() {
        return ctx;
    }
}

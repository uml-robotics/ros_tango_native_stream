/*
 * Copyright (c) 2014, University Of Massachusetts Lowell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Massachusetts Lowell nor the names
 * from of its contributors may be used to endorse or promote products
 * derived this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * Author: Eric McCann <emccann@cs.uml.edu>
*/

package edu.uml.tango.tango_root.peanut_stream;

import android.util.Log;
import android.util.SparseArray;

import org.ros.message.Duration;
import org.ros.message.Time;

import java.util.ArrayList;
import java.util.HashMap;

public class RateWatcher {
    private SparseArray<RateHandle> awareMap = new SparseArray<RateHandle>();

    private final RateUpdater mUpdater;

    public RateWatcher(RateUpdater ru)
    {
        mUpdater = ru;
    }

    public RateProvider add(int i)
    {
        final RateHandle r = new RateHandle(i);
        RateProvider rp = new RateProvider() {
            @Override
            public void addStamp(Time t) {
                r.add(t);
            }
        };
        awareMap.put(i,r);
        return rp;
    }

    public double getRate(int i)
    {
        return awareMap.get(i).getRate();
    }

    public class RateHandle {
        private static final int WINDOW_LENGTH = 10;
        private ArrayList<Duration> durations = new ArrayList<Duration>();
        private Time mLastTime;
        int mId;
        public RateHandle(int id)
        {
            mId = id;
        }
        public void add(Time t)
        {
            synchronized(this) {
                if (mLastTime != null) {
                    durations.add(t.subtract(mLastTime));
                }
                while (durations.size() > WINDOW_LENGTH)
                {
                    durations.remove(0);
                }
                mLastTime = t;
            }
            mUpdater.update(mId);
        }

        private final double SEC_PER_NSEC = Math.pow(10,-9);
        public float getRate()
        {
            double secs = 0;
            int count = 0;
            synchronized(this) {
                for (Duration d : durations)
                {
                    secs += (double)d.secs + ((double)d.nsecs*SEC_PER_NSEC);
                }
                count = durations.size();
            }
            secs /= (1.0*count);
            return (float)(1.0/secs);
        }
    }

    public interface RateProvider
    {
        public void addStamp(org.ros.message.Time t);
    }

    public interface RateUpdater
    {
        public void update(int id);
    }
}

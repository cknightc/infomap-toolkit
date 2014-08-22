/*
 * Part of infomap-toolkit--a java based concurrent toolkit for running the
 * infomap algorithm (all credit for the algorithm goes to Martin Rosvall and
 * Carl T. Bergstrom).
 * 
 * Copyright (C) 2014 Zach Tosi
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package concurrency_tools;

import java.util.concurrent.BlockingQueue;

public class Consumer implements Runnable {

    private volatile boolean continueRunning = true;

    private final BlockingQueue<Task> taskQueue;

    public Consumer(BlockingQueue<Task> taskQueue) {
        this.taskQueue = taskQueue;
    }

    @Override
    public void run() {
        while (continueRunning) {
            try {
                taskQueue.take().perform();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void shutdown() {
        continueRunning = false;
    }

}

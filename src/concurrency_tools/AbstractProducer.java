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

/**
 * A generalized task producer which should be extended for some specific map
 * operation which uses the producer/consumer framework.
 * 
 * @author Zach Tosi
 *
 */
public abstract class AbstractProducer implements Runnable {

    protected final Consumer[] consumers;

    public AbstractProducer(Consumer[] consumers) {
        this.consumers = consumers;
    }

    public void shutdownConsumers() {
        for (Consumer c : consumers) {
            c.shutdown();
        }
    }

}

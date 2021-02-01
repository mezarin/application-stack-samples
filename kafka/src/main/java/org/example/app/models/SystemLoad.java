/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
package org.example.app.models;

import java.util.Objects;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

public class SystemLoad {

    private static final Jsonb jsonb = JsonbBuilder.create();

    public String hostname;
    public Double loadAverage;

    public SystemLoad(String hostname, Double cpuLoadAvg) {
        this.hostname = hostname;
        this.loadAverage = cpuLoadAvg;
    }

    public SystemLoad() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SystemLoad))
            return false;
        SystemLoad sl = (SystemLoad) o;
        return Objects.equals(hostname, sl.hostname) && Objects.equals(loadAverage, sl.loadAverage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, loadAverage);
    }

    @Override
    public String toString() {
        return "CpuLoadAverage: " + jsonb.toJson(this);
    }
}
/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit;

import kintsugi3d.builder.fit.settings.ReadonlyBasisSettings;

public class SimpleBasisSettings implements ReadonlyBasisSettings
{
    private final int basisCount;
    private final int disabledBasisCount;
    private final int basisResolution;

    public SimpleBasisSettings(int basisCount, int disabledBasisCount, int basisResolution)
    {
        this.basisCount = basisCount;
        this.disabledBasisCount = disabledBasisCount;
        this.basisResolution = basisResolution;
    }

    @Override
    public int getBasisCount()
    {
        return this.basisCount;
    }

    @Override
    public int getDisabledBasisCount()
    {
        return disabledBasisCount;
    }

    @Override
    public int getBasisResolution()
    {
        return this.basisResolution;
    }
}

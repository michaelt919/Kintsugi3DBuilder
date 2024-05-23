/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package org.jengineering.sjmply;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

/** A functional interface for either reading or writing the data of a certain element property.
 *  
 *  @author Dirk Toewe
 */
interface PropertyIO
{
  /** Reads or writes the property data for the element with the given index.
   *  
   *  @param element The index of the element for which data is either read or written.
   *  @throws IOException If a problem is encountered while reading or writing.
   */
  void process( int element ) throws IOException;

  default PropertyIO andThen( PropertyIO reader )
  {
    requireNonNull(reader);
    return elem -> {
      this  .process(elem);
      reader.process(elem);
    };
  }
}
package org.jengineering.sjmply;
/* Copyright 2016 Dirk Toewe
 * 
 * This file is part of org.jengineering.sjmply.
 *
 * org.jengineering.sjmply is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * org.jengineering.sjmply is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with org.jengineering.sjmply. If not, see <http://www.gnu.org/licenses/>.
 */

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
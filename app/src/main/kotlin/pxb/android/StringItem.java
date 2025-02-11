/*
 * Copyright (c) 2009-2013 Panxiaobo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pxb.android;

import androidx.annotation.NonNull;

public class StringItem {
  public String data;
  public int dataOffset;
  public int index;

  public StringItem() {
    super();
  }

  public StringItem(String data) {
    super();
    this.data = data;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    StringItem other = (StringItem) obj;
    if (data == null) {
      return other.data == null;
    } else return data.equals(other.data);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((data == null) ? 0 : data.hashCode());
    return result;
  }

  @NonNull
  public String toString() {
    return String.format("S%04d %s", index, data);
  }

}

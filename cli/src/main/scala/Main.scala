/* Copyright 2019-2020 EPFL, Lausanne */

/* Copyright 2019-2020 EPFL IC LARA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stainlessfit
package cli

import core.App

object Main {
  def main(args: Array[String]): Unit = {
    ConfigParser.parse(args.toIndexedSeq).foreach(App.launch)
  }
}

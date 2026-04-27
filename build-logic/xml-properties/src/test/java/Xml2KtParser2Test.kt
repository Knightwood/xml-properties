/*
 * Copyright [2023-2024] [KnightWood]
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
 *
 */

interface IExample {

}

abstract class Example1 {
    public open var a: Int = 1
        get() {
            return field
        }
    set(value) {
        field = value
    }
}

open class Example2 {

}

class Example3 : Example2() {

}

object Example4 {

}
 inline fun<reified T> a(){

}

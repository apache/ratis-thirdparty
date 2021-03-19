<!--
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. See accompanying LICENSE file.
-->

# Apache Ratis Thirdparty

All bundled thirdparty dependencies are centralized in the *ratis-thirdparty* module
and the *ratis-thirdparty-hadoop* module.
These modules are located in a separated repository (https://github.com/apache/ratis-thirdparty)
but not attached to the core Apache Ratis repository (https://git-wip-us.apache.org/repos/asf?p=ratis.git)
as they only need to change when one of these dependencies are changed.
All dependencies included in ratis-thirdparty/ratis-thirdparty-hadoop
must be relocated to a different package to ensure no downstream classpath pollution.

See also: https://github.com/apache/ratis/blob/master/BUILDING.md
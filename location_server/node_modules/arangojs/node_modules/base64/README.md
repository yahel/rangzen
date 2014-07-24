base64
======

base64 encoder / decoder 

Usage
=====

```
> a = require('./index')
{ encode: [Function],
  decode: [Function],
  encodeURL: [Function],
  decodeURL: [Function] }

> a.encode("fool")
'Zm9vbA=='

> a.decode('Zm9vbA==')
'fool'

> a.decodeURL('aGVsbG8gd29ybGQ_')
'hello world?'
```

License
=======
```
Copyright (c) 2013 Kaerus (kaerus.com), Anders Elo <anders @ kaerus com>.
```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
Cloudify-iTests-Deployer
========================


Copyright and license
----------------------
Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");<br/>
you may not use this file except in compliance with the License.<br/>
You may obtain a copy of the License at 

       http://www.apache.org/licenses/LICENSE-2.0
     
Unless required by applicable law or agreed to in writing, software<br/>
distributed under the License is distributed on an "AS IS" BASIS,<br/>
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br/>
See the License for the specific language governing permissions and<br/>
limitations under the License.


Manual
----------------------

To upgrade the cloudify tester version:

1.  remove the build from to /export/tgrid/itests-deployer/cloudify-home
2.  move the new build into the same location
3.  copy ec2-cloud.groovy and ec2-cloud.properties from /export/tgrid/itests-deployer/Cloudify-iTests-Deployer/src/main/resources to /export/tgrid/itests-deployer/cloudify-home/clouds/ec2
4.  put a *.pem file in the upload folder of the used cloud

To run the test suite:

1.  cd into /export/tgrid/itests-deployer/Cloudify-iTests-Deployer/src/main/java
2.  (optional) change the mysql username and password to be used in the deployer.properties file
2.  run groovy with all the arguments e.g.
`groovy itests_deployer.groovy 4980-74 2.6.0 m1 2 CLOUDIFY REGULAR
\**/BadUSMServiceDownAfterUninstallApplicationTest.class,\**/DeploymentsControllerTest.class
\**/xen/\**,**/*Abstract*,**/cloud/**,**/cli/cloudify/pu/**,**/cli/cloudify/AdminApiControllerTest**,**/cli/cloudify/security/**
eu-west-1 ec2,openstack,rsopenstack,byon dummy dummy dummy dummy dummy dummy 2.6.0-SNAPSHOT`
3.  the argument list by order is:
	
	1. build number
	2. version
	3. milestone
	4. suite number (the number to divied the suite by)
	5. suite name
	6. suite type
	7. include tests regex
	8. exclude tests regex
	9. ec2 region
	10. suppported clouds
	11. byon machines
	12. branch name
	13. package name
	15. xap jdk
	16. sgtest jdk
	17. s3 cloudify publish folder

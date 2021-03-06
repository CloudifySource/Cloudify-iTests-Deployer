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

### To upgrade the cloudify tester version:

1.  remove the build from to `/export/tgrid/itests-deployer/cloudify-home`
2.  move the new build into the same location
3.  copy `ec2-cloud.groovy` and `ec2-cloud.properties` from `/export/tgrid/itests-deployer/Cloudify-iTests-Deployer/src/main/resources` to `/export/tgrid/itests-deployer/cloudify-home/clouds/ec2`
4.  copy `pre-bootstrap.sh` from `/export/tgrid/itests-deployer/Cloudify-iTests-Deployer/src/main/resources` to `/export/tgrid/itests-deployer/cloudify-home/clouds/ec2/upload/cloudify-overrides`
5.  put a *.pem file in the upload folder of the used cloud
6.  fill in the credentials details in `ec2-cloud.properties`
7.  make sure the license is valid and copy it to the cloud's cloudify-overrides dir (Big Data)

Note: make sure all groovy scripts (mysql,tomcat and all groovy scripts in itests-service) compiles with the new version.
Possible solution: use mysql,tomcat from the updated cloudify-recipes.


### To run the test suite:

1.  cd into `/export/tgrid/itests-deployer/Cloudify-iTests-Deployer/src/main/groovy`
2.  (optional) change the mysql username and password to be used in the `deployer.properties` file
2.  run groovy with all the arguments e.g.

	```shell
	groovy itests_deployer.groovy 4980-74 2.6.0 m1 2 CLOUDIFY Cloudify-Regression
	**/BadUSMServiceDownAfterUninstallApplicationTest.class,**/DeploymentsControllerTest.class
	**/xen/**,**/*Abstract*,**/cloud/**,**/cli/cloudify/pu/**,**/cli/cloudify/AdminApiControllerTest**,**/cli/cloudify/security/**
	eu-west-1 dummy dummy dummy 2.6.0-SNAPSHOT 9.6.0-SNAPSHOT-20130609.b9483-871 2.6.0-SNAPSHOT-20130609.b4983-348 dummy
	```

3.  the argument list by order is:
	
	1. build number
	2. version
	3. milestone
	4. suite number (the number to divide the suite by)
	5. suite name
	6. suite type
	7. include tests regex
	8. exclude tests regex
	9. ec2 region
	10. byon machines
	11. branch name
	12. package name
	13. s3 cloudify publish folder
	14. XAP maven version
	15. Cloudify maven version
	16. maven local repository folder


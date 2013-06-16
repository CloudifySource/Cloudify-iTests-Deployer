application {
	name="iTests-Management"

    /*service {
        name = "space"
    }*/


	service {
		name = "mysql"
	}

	service {
		name = "tomcat"
		dependsOn = ["mysql"]
	}
}

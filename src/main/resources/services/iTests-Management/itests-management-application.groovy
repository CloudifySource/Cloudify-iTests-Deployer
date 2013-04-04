application {
	name="iTests-Management"

	service {
		name = "mysql"
	}

	service {
		name = "tomcat"
		dependsOn = ["mysql"]
	}
}

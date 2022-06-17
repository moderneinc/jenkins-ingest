pipeline {
    agent any

    stages {
        stage('Hello') {
            steps { 
                script {
                    def jobName = input message: "Please enter the ingest job expression (relative to ingest/) to run" ,
                     parameters: [string(defaultValue: 'jenkinsci',
                                  description: '',
                                  name: 'Job Expression')]
                    def jobExpression = "ingest/${jobName}"

                    def jobLimitString = input message: "Max number of jobs to queue" ,
                     parameters: [string(defaultValue: '10',
                                  description: '',
                                  name: 'Job Count')]


                    Integer jobLimit = jobLimitString as Integer
                    def jobs = hudson.model.Hudson.getInstance().getAllItems(Job.class).findAll {it.getFullName().contains(jobExpression)}
                    for (int i = 0; i < jobs.size(); i++) {
                        if (i == jobLimit) {
                            break;
                        }
                        build wait: false, job: jobs[i].getFullName()
                    }
                }
            }
        }
    }
}

pipeline {
    agent any

    stages { 
        stage('java jdk setup') {
            //Herunterladen und Entpacken des Java JDKs
            steps {
                sh 'wget https://download.java.net/java/jdk8u192/archive/b04/binaries/jdk-8u192-ea-bin-b04-linux-x64-01_aug_2018.tar.gz'
                sh 'tar xvf jdk-8*.tar.gz'
                script{
                    JAVA_HOME='jdk1.8.0_192'
                }
            }
        }

        stage('android sdk setup'){
            //Herunterladen und Entpacken des Android SDKs
            steps{
                sh 'wget https://dl.google.com/android/repository/commandlinetools-linux-6858069_latest.zip'
                sh 'unzip -o commandlinetools-linux-6858069_latest.zip'
                script {
                    sh 'mkdir -p sdkTools'
    	            env.ANDROID_HOME = "./sdkTools"
                    env.PATH = "tools:tools/bin:${env.PATH}"
	            env.ANDROID_SDK_ROOT = "./sdkTools"
                }   
                sh 'yes | cmdline-tools/bin/sdkmanager --licenses --sdk_root=./sdkTools'
            }
        }

        stage('Build') {
            steps {
                //Klonen des Git Repositories und Bau der APK
                git 'https://github.com/LizardWithHat/slic-android.git'
                sh 'chmod 755 ./gradlew'
                withGradle {
                    sh './gradlew build :app:assembleDebug -x lint'
                }
            }

            post {
                success {
                    //Speicher die APK in Jenkins
                    archiveArtifacts 'app/build/outputs/apk/debug/*.apk'
                    //Upload eines GitHub Releases fehlt hier, aus Sicherheitsgründen
                    //Generell aber über die GitHub API möglich, siehe https://docs.github.com/en/rest/reference/repos#create-a-release
                }
            }
        }
    }    
}
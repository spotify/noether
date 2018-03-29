@Grab(group='com.spotify', module='pipeline-conventions', version='1.0.8')

import com.spotify.pipeline.Pipeline

new Pipeline(this) {{ build {
    notify.byMail(recipients: 'flatmap-squad@spotify.com')

    group(name: 'Build') {
        jenkinsPipeline.inJob {
            steps {
                shell('sbt -no-colors test')
            }
        }
    }

}}}
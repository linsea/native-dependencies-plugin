/*
* Copyright (C) 2016 Author <dictfb#gmail.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.github.linsea.android

import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.util.regex.Matcher
import java.util.regex.Pattern


class NativeSoFilesCollectorTask extends DefaultTask {

    final Logger log = Logging.getLogger NativeSoFilesCollectorTask

    @OutputDirectory
    def File jniLibs = project.android.sourceSets.main.jniLibs.srcDirs.last() //MUST last one

    @Input
    def Set<File> dependencies //所有.so依赖文件

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        log.info "jniLibs=${jniLibs.absolutePath}"

        if (!inputs.incremental) {
            project.delete(jniLibs.listFiles())
        }

        project.nativeso.soFilters.each { String abisFilter ->
            log.info "resolve ${abisFilter} .so dependencies"
            dependencies.findAll { File f -> f.name.contains(abisFilter) }.each { File file ->
                log.info "found .so file: ${file.absolutePath}"
                RenamingBean renamingBean = findNamingBean(file.name)
                if (renamingBean != null) {
                    log.info "match renaming rule [${renamingBean}] for file:${file.name}"
                    String prefix = (renamingBean == null || renamingBean.prefix == null) ? '' : renamingBean.prefix
                    String regex = (renamingBean != null ? renamingBean.regex : '')
                    String replaceWith = (renamingBean != null ? renamingBean.replaceWith : null)
                    String replaceAll = (renamingBean != null ? renamingBean.replaceAll : null)

                    if (replaceAll != null) {//replaceAll is over replaceWith
                        project.copy {
                            from file
                            into jniLibs.absolutePath + File.separator + abisFilter
                            rename { String fileName -> prefix + fileName.replaceAll(regex, replaceAll) }
                        }
                    } else {
                        project.copy {
                            from file
                            into jniLibs.absolutePath + File.separator + abisFilter
                            rename regex, prefix + replaceWith + '.so'
                        }
                    }
                } else {
                    log.info "no match renaming rule for file:${file.name}, use orignal file name."
                    project.copy {
                        from file
                        into jniLibs.absolutePath + File.separator + abisFilter
                    }
                }
            }
        }

    }

    def findNamingBean(String filename) {
        List<RenamingBean> ruleList = project.nativeso.renamingList
        for (int i = 0; i < ruleList.size(); i++) {
            RenamingBean bean = ruleList.get(i)
            if (bean.regex != null && bean.regex != '') {
                Pattern pattern = Pattern.compile(bean.regex)
                Matcher matcher = pattern.matcher(filename)
                if (bean.replaceAll != null) {
                    if (matcher.find())
                        return bean
                } else {
                    if (matcher.matches()) {
                        return bean
                    }
                }
            }
        }
        return null
    }
}

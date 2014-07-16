maxlen = 100
def CheckChange(input_api, output_api):
    results = []

    # Check for presence of DO\ NOT\ SUBMIT in files to be committed.
    results += input_api.canned_checks.CheckDoNotSubmit(input_api, output_api)

    # Checks that all input files do not contain any <TAB> characters.
    results += input_api.canned_checks.CheckChangeHasNoTabs(input_api, 
                                                            output_api)

    # Checks for lines longer than maxlen.
    results += input_api.canned_checks.CheckLongLines(input_api, 
                                                      output_api, 
                                                      maxlen)

    # Clean and build the app before linting it so that the currect and current
    # .class files are present for lint.
    # BuckClean(input_api, output_api)
    results += BuckBuild(input_api, output_api)

    # Lint.
    results += RunAndroidLint(input_api, output_api)

    # Run the unit tests.
    # TODO(lerner); Add this.
    # results += buckTest(input_api, output_api)

    return results

# Runs `buck clean`, which deletes all built files in buck-out.
def BuckClean(input_api, output_api):
  source_directory = str(input_api.PresubmitLocalPath())
  args = 'buck clean'.split()
  env = input_api.environ.copy()
  subproc = input_api.subprocess.Popen(
      args,
      cwd=source_directory,
      env=env,
      stdin=input_api.subprocess.PIPE,
      stdout=input_api.subprocess.PIPE,
      stderr=input_api.subprocess.STDOUT)
  stdout_data = subproc.communicate()[0]

# Runs `buck build rangzen` which compiles an APK of the app (and all of
# its prerequisites).
# Runs before Android Lint so that the appropriate .class files can be linted.
def BuckBuild(input_api, output_api):
  source_directory = str(input_api.PresubmitLocalPath())
  buildTargetName = 'experimentalApp'
  args = ('buck build %s' % (buildTargetName,)).split()
  env = input_api.environ.copy()
  subproc = input_api.subprocess.Popen(
      args,
      cwd=source_directory,
      env=env,
      stdin=input_api.subprocess.PIPE,
      stdout=input_api.subprocess.PIPE,
      stderr=input_api.subprocess.STDOUT)
  stdout_data = subproc.communicate()[0]
  # did_build_apk = lambda line: (input_api.re.match(r'^built APK', line))
  did_not_build_apk = lambda line: (input_api.re.match(r'^BUILD FAILED:', line))
  build_failure_lines = filter(did_not_build_apk, stdout_data.splitlines())
  if build_failure_lines:
    return [output_api.PresubmitError('Did not build Rangzen APK successfully.')]
  else:
    return [output_api.PresubmitNotifyResult('Built Rangzen APK successfully.')]

# Runs Android lint on the project, manually specifying the java sources, test
# sources, resources and classpath (compiled .class files locations).
def RunAndroidLint(input_api, output_api):
  error, stdout_data = LintSourceDirectories('java/org/denovogroup/rangzen', 
                                             'tests/rog/denovogroup/rangzen',
                                             input_api, output_api)
  if error:
    return [output_api.PresubmitError('Lint error\n%s' % '\n'.join(error),
                                     long_text=stdout_data)]
  else:
    return [output_api.PresubmitNotifyResult('Android lint is clean.')]
  pass 

# Utility function which lints the given java and tests directories.
def LintSourceDirectories(java_directory, tests_directory, input_api, output_api):
  repo_directory = input_api.PresubmitLocalPath()
  resources_directory = 'res/org/denovogroup/rangzen/res'
  classes_directory = 'buck-out'
  command = ('lint %s --sources %s --sources %s --classpath %s --resources %s -Wall' %
             (repo_directory,
              java_directory, 
              tests_directory,
              classes_directory,
              resources_directory))
  args = command.split()
  env = input_api.environ.copy()
  subproc = input_api.subprocess.Popen(
      args,
      cwd=repo_directory,
      env=env,
      stdin=input_api.subprocess.PIPE,
      stdout=input_api.subprocess.PIPE,
      stderr=input_api.subprocess.STDOUT)
  stdout_data = subproc.communicate()[0]
  is_error_summary = lambda line: (input_api.re.match(r'^\d+ errors, \d+ warnings', line))
  error = filter(is_error_summary, stdout_data.splitlines())
  if error:
    return error, stdout_data + input_api.PresubmitLocalPath()

  return [], stdout_data

# This method is run when git cl upload is run.
def CheckChangeOnUpload(input_api, output_api):
    return CheckChange(input_api, output_api)

# This method is run on git cl push.
def CheckChangeOnCommit(input_api, output_api):
    return CheckChange(input_api, output_api)

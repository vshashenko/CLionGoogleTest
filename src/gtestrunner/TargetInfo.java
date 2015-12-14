package gtestrunner;

class TargetInfo
{
    public String runTargetProjectName;
    public String runTargetName;
    public String programParams;
    public String configName;
    public String workingDir;

    public ExecutableInfo executableInfo;

    @Override
    public String toString()
    {
        return runTargetName;
    }
}

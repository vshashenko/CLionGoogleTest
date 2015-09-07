package gtestrunner;

public interface IProcessOutputObserver
{
    void onProcessCreated(Process process);

    void onNewLine(String s);
}

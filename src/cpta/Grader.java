package cpta;

import cpta.environment.Compiler;
import cpta.environment.Executer;
import cpta.exam.ExamSpec;
import cpta.exam.Problem;
import cpta.exam.Student;
import cpta.exam.TestCase;
import cpta.exceptions.CompileErrorException;
import cpta.exceptions.FileSystemRelatedException;
import cpta.exceptions.InvalidFileTypeException;
import cpta.exceptions.RunTimeErrorException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Grader {
    Compiler compiler;
    Executer executer;

    public Grader(Compiler compiler, Executer executer) {
        this.compiler = compiler;
        this.executer = executer;
    }

    public Map<String,Map<String, List<Double>>> gradeSimple(ExamSpec examSpec, String submissionDirPath) {
        // TODO Problem 1-1
        //
        Map<String, Map<String, List<Double>>> studentAndResult = new TreeMap<>();

        try {
            for (int i = 0; i < examSpec.students.size(); i++) { // loop through student
                //  problemId, scoreList
                Map<String, List<Double>> problemAndScore = new TreeMap<>();
                Student thisStudent = examSpec.students.get(i);

                for (int j = 0; j < examSpec.problems.size(); j++) { // loop through problem
                    String fullPath = new String(makePath(examSpec, submissionDirPath, i, j));
                    Problem thisProblem = examSpec.problems.get(j);
                    compiler.compile(fullPath + thisProblem.targetFileName);

                    List<Double> scoreList = new ArrayList<>();

                    for (int k = 0; k < thisProblem.testCases.size(); k++) { //loop through testcases

                        TestCase thisCase = thisProblem.testCases.get(k);

                        String studentOutputString = new String();
                        String referenceOutputString = new String();

                        String targetFilePath = fullPath + thisProblem.id + ".yo";
                        String inputFilePath = thisProblem.testCasesDirPath + thisCase.inputFileName;
                        String referenceOutputFilePath = thisProblem.testCasesDirPath + thisCase.outputFileName;
                        String outputFilePath = fullPath + thisCase.outputFileName;

                        executer.execute(targetFilePath, inputFilePath, outputFilePath);

                        try {
                            studentOutputString = Files.readString(Path.of(outputFilePath));
                            referenceOutputString = Files.readString(Path.of(referenceOutputFilePath));
                        } catch (IOException e) {
                            e.getMessage();
                        }

                        if (studentOutputString.equals(referenceOutputString)){
                            scoreList.add(thisCase.score);
                        } else {
                            scoreList.add(0.0);
                        }


                    }
                    problemAndScore.put(thisProblem.id, scoreList);
                }
                studentAndResult.put(thisStudent.id,problemAndScore);
            }
        } catch (CompileErrorException|InvalidFileTypeException|FileSystemRelatedException|RunTimeErrorException e) {
            e.getMessage();
        }


        return studentAndResult;
    }




    public Map<String,Map<String, List<Double>>> gradeRobust(ExamSpec examSpec, String submissionDirPath) {
        // TODO Problem 1-2
        Map<String, Map<String, List<Double>>> studentAndResult = new TreeMap<>();


            for (int i = 0; i < examSpec.students.size(); i++) { // loop through students
                //  problemId, scoreList
                Map<String, List<Double>> problemAndScore = new TreeMap<>();
                Student thisStudent = examSpec.students.get(i);


                for (int j = 0; j < examSpec.problems.size(); j++) { // loop through problems
                    boolean wrongCompile = false;
                    // full path has such form : data/exam-robust/2020-12345/problem1/
                    String fullPath = new String(makePath(examSpec, submissionDirPath, i, j));
                    Problem thisProblem = examSpec.problems.get(j);

                    //Group4 - No submission
                    File checkSubmissionDir = new File(fullPath);
                    boolean directoryExistenceChecker = checkSubmissionDir.isDirectory();


                    if (directoryExistenceChecker){
                    copyAndPastAdditionalDir (fullPath);
                    }

                    // We should Copy and Paste all files in wrappersDirPath if it's not null
                    if (thisProblem.wrappersDirPath != null && directoryExistenceChecker == true){
                        copyAndPastWrapper(thisProblem.wrappersDirPath,fullPath);
                    }


                    try {
                    // Compile target .sugo file
                    compiler.compile(fullPath + thisProblem.targetFileName);
                    // compile all sugo files
                    compileAll (fullPath);

                    } catch (CompileErrorException e) {
                        e.printStackTrace();
                        wrongCompile = true;
                    } catch (InvalidFileTypeException|FileSystemRelatedException e){
                        e.printStackTrace();
                    }


                    List<Double> scoreList = new ArrayList<>();

                    for (int k = 0; k < thisProblem.testCases.size(); k++) { //loop through testcases

                        TestCase thisCase = thisProblem.testCases.get(k);

                        boolean case_Insensitive = false;
                        boolean ignore_Whitespaces = false;
                        boolean trailing_Whitespaces = false;

                        if (thisProblem.judgingTypes != null){
                            case_Insensitive = thisProblem.judgingTypes.contains(Problem.CASE_INSENSITIVE);
                            ignore_Whitespaces = thisProblem.judgingTypes.contains(Problem.IGNORE_WHITESPACES);
                            trailing_Whitespaces = thisProblem.judgingTypes.contains(Problem.TRAILING_WHITESPACES);
                        }

                        String studentOutputString = new String();
                        String referenceOutputString = new String();

                        String targetFilePath = fullPath + thisProblem.id + ".yo";
                        String inputFilePath = thisProblem.testCasesDirPath + thisCase.inputFileName;
                        String referenceOutputFilePath = thisProblem.testCasesDirPath + thisCase.outputFileName;
                        String outputFilePath = fullPath + thisCase.outputFileName;



                        // execute compiled .yo file
                        boolean wrongRuntime = false;
                        try {
                        executer.execute(targetFilePath, inputFilePath, outputFilePath);
                        } catch (RunTimeErrorException e) {
                            e.printStackTrace();
                            wrongRuntime = true;
                        } catch (InvalidFileTypeException | FileSystemRelatedException e) {
                            e.printStackTrace();
                        }

                        File checkSubmissionFile = new File(fullPath + thisProblem.targetFileName);
                        boolean fileExistenceChecker = checkSubmissionFile.isFile();
                        File checkExecutionFile = new File(fullPath + thisProblem.id + ".yo");
                        boolean yoExistenceChecker = checkExecutionFile.isFile();

                        // Read result of student and reference
                        try {
                            studentOutputString = Files.readString(Path.of(outputFilePath));
                            referenceOutputString = Files.readString(Path.of(referenceOutputFilePath));
//                            System.out.printf("%s, %s\n", studentOutputString, referenceOutputString);

                        } catch (IOException e) {
                            e.getMessage();
                        }


                        // Score by comparing 2 Strings
                        // first we need to preprocess depending on judging Type
                        studentOutputString = preProcessing(studentOutputString, case_Insensitive, ignore_Whitespaces, trailing_Whitespaces);
                        referenceOutputString = preProcessing(referenceOutputString, case_Insensitive, ignore_Whitespaces, trailing_Whitespaces);
//                        System.out.printf("%s, %s\n", studentOutputString, referenceOutputString);
                        //here we actually score the score for each problem
                        if (wrongCompile || wrongRuntime) {
                            scoreList.add(0.0);
                        } else if (!fileExistenceChecker && yoExistenceChecker){
                            if (studentOutputString.equals(referenceOutputString)){
                                scoreList.add(thisCase.score/2);
                            }
                        } else if (!directoryExistenceChecker || !fileExistenceChecker) {
                            scoreList.add(0.0);
                        } else if (studentOutputString.equals(referenceOutputString)){
                            scoreList.add(thisCase.score);
                        } else {
                            scoreList.add(0.0);
                        }
                    }
                    problemAndScore.put(thisProblem.id, scoreList);
                }
                studentAndResult.put(thisStudent.id,problemAndScore);
            }



        return studentAndResult;
    }



    String makePath(ExamSpec examSpec, String submissionDirPath, int i, int j) {
        String fullPath = "";
        fullPath = submissionDirPath + examSpec.students.get(i).id + "/";
        File trollCheck = new File(fullPath);
        if (trollCheck.isDirectory() == false) {
            File trollRepair = new File(submissionDirPath);
            String[] dirList = trollRepair.list();
            for (String dirName : dirList) {
                if (dirName.startsWith(examSpec.students.get(i).id)) {
                    fullPath = submissionDirPath + dirName +"/";
                }
            }
        }
        fullPath += examSpec.problems.get(j).id+"/";
        return fullPath;
    }

    void copyAndPastWrapper (String wrapperFilePath, String destPath) {
        File wrapperFile = new File (wrapperFilePath);
        String[] wrapperFileList = wrapperFile.list(sugoFilter);
        for (String wrapper : wrapperFileList) {
            try{
                copyFileUsingStream(wrapperFilePath+wrapper, destPath+wrapper);
            } catch (IOException e) {
                e.getMessage();
            }
        }
    }

    void copyAndPastAdditionalDir (String fullPath) {
        File file = new File (fullPath);
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        if (directories.length == 0) return;
        for (String subDirectory : directories) {
            try{
                File subDirFile = new File(fullPath  + subDirectory);
                String[] subDirFileList = subDirFile.list();
                if (subDirFileList == null) return;
                for (String subDirFileName : subDirFileList){
                copyFileUsingStream(fullPath  + subDirectory+"/"+ subDirFileName, fullPath + subDirFileName);
                }
            } catch (IOException e) {
                e.getMessage();
            }
        }
    }

    void compileAll (String fullPath) throws FileSystemRelatedException, InvalidFileTypeException, CompileErrorException {
        File f = new File (fullPath);
        String[] fileList = f.list(sugoFilter);
        for (String name : fileList) {
            try {
                compiler.compile(fullPath + name);
            } catch (CompileErrorException e) {
                throw e;
            } catch (InvalidFileTypeException|FileSystemRelatedException e){
                throw e;
            }
        }
    }

    String preProcessing(String result, boolean case_Insensitive, boolean ignore_Whitespaces, boolean trailing_Whitespaces) {
        String returnString = new String(result);
        if (case_Insensitive) {
            result = result.toLowerCase();

        }
        if (ignore_Whitespaces) {
            result = result.replaceAll("\\s","");
        }
        if (trailing_Whitespaces) {
            while (result.endsWith(" ") || result.endsWith("\n") || result.endsWith("\t")){
               result = result.replaceFirst("\\s++$", "");
            }
        }
        returnString = result;
        return returnString;
    }

    private static void copyFileUsingStream(String source, String dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;

        try {
            is = new FileInputStream(new File(source));
            os = new FileOutputStream(new File(dest));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    FilenameFilter sugoFilter = new FilenameFilter() {
        @Override
        public boolean accept(File f, String name) {
            return name.endsWith(".sugo");
        }
    };
}


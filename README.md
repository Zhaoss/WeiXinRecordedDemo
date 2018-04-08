## v3.0 增加点击拍照功能, 优化项目结构
## v2.5 增加剪切时长功能, 优化交互
## v2.4 大幅优化, 使用前后摄像头拍摄视频时, 合成视频过慢的问题 (感谢@bertsir)
## v2.3 增加摄像头切换和闪光灯开启功能, 修复前置摄像头拍摄出来画面颠倒等bug
<br /> 

```
功能概括: 

1.基于ffmpeg的视频分段拍摄及合成;

2.自定义拍摄按钮, 显示多段视频拍摄进度, 和删除视频段落模式;

3.自定义view, 实现手绘涂鸦;

4.自定义可触摸旋转缩放位移的表情文字view;

5.仿微信裁剪图片控件, 自定义View实现功能;

6.基于ffmpeg改变视频速度(加速或者减速);

7.基于ffmpeg裁剪视频宽高;

8.基于ffmpeg的图片和视频合成处理;

9.基于ffmpeg剪切视频时长.
```

# 如果想了解实现思路, 这里有详细源码分析讲解

<a href="http://www.jianshu.com/p/5a173841a828" target="_blank">仿微信视频拍摄UI, 基于ffmpeg的视频录制编辑(上)</a>

<a href="http://www.jianshu.com/p/df568b7141c5" target="_blank">仿微信视频拍摄UI, 基于ffmpeg的视频录制编辑(下)</a>

![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/Image/demo1.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/Image/demo2.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/Image/demo3.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/Image/demo4.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/Image/demo5.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/Image/demo6.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/Image/demo7.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/Image/demo8.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/Image/demo9.png?raw=true)

### 本项目所使用的so库是VCamera，个人免费， 禁止商用，只用作demo演示

* * *   
<br /> 

# MIT License
Copyright (c) 2017 Zhaoss (838198688@qq.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

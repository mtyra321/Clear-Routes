package edu.byui.mynavigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import java.util.*

class TabAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    private  var  mFragmentList: Vector<Fragment> = Vector()
    private  var mFragmentTitleList: Vector<String> = Vector()
    override fun getItem(position: Int): Fragment {
        return mFragmentList[position]
    }

    override fun getCount(): Int {
        return mFragmentList.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return "Tab " + (position + 1)
    }
    fun addFrag(fragment: Fragment?, title: String?) {
        if (fragment != null) {
            mFragmentList.add(fragment)
        }
        if (title != null) {
            mFragmentTitleList.add(title)
        }
    }
    fun removeAllFragments() {
        mFragmentList.removeAllElements()
        mFragmentTitleList.removeAllElements()

        notifyDataSetChanged()
    }
    fun removeFragment(index: Int) {
        mFragmentList.removeAt(index)
        mFragmentTitleList.removeAt(index)

        notifyDataSetChanged()
    }
     override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }
}
